package org.ouracademy.ovejafiel.whatsapp.command.disconnect;

import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The pastor cuts the church's WhatsApp. It is marked here first, and then the number is removed
 * from the provider: without that the number would stay connected there (and at Meta), the agent
 * would keep receiving its messages, and connecting again would find the number taken.
 */
@Slf4j
@Service
class DisconnectCommandHandlerImpl implements DisconnectCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final WhatsAppGateway gateway;
  private final TransactionTemplate transaction;

  DisconnectCommandHandlerImpl(
      WhatsAppConnectionRepository connections,
      FindChurchOfOrganizationQueryHandler findChurch,
      WhatsAppGateway gateway,
      TransactionTemplate transaction) {
    this.connections = connections;
    this.findChurch = findChurch;
    this.gateway = gateway;
    this.transaction = transaction;
  }

  private record Marked(boolean wasConnected, String phoneNumberId) {}

  @Override
  public DisconnectResult handle(DisconnectCommand command) {
    var marked = transaction.execute(status -> mark(command));
    // After the commit and outside the transaction: the provider answers with a "deleted" event
    // that finds the connection already cut and leaves it as the pastor left it.
    if (marked.wasConnected() && marked.phoneNumberId() != null) {
      removeFromProvider(marked.phoneNumberId());
    }
    return new DisconnectResult(marked.wasConnected());
  }

  private Marked mark(DisconnectCommand command) {
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var connection =
        connections
            .findById(churchId)
            .orElseThrow(() -> new NoSuchElementException("The church has no WhatsApp"));
    var wasConnected = connection.isConnected();
    connection.disconnect();
    connections.save(connection);
    return new Marked(wasConnected, connection.getPhoneNumberId());
  }

  /** A failure here does not undo the disconnection: it is logged so someone can finish by hand. */
  private void removeFromProvider(String phoneNumberId) {
    try {
      gateway.deleteNumber(phoneNumberId);
    } catch (RuntimeException e) {
      log.warn(
          "WhatsApp number {} is disconnected here but could not be removed from the provider"
              + " ({}): remove it in Kapso or in the Business app (Linked platforms)",
          phoneNumberId,
          e.getMessage());
    }
  }
}
