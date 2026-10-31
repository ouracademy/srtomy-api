package org.ouracademy.ovejafiel.whatsapp.command.disconnect;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DisconnectCommandHandlerImpl implements DisconnectCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final FindChurchOfOrganizationQueryHandler findChurch;

  @Override
  @Transactional
  public DisconnectResult handle(DisconnectCommand command) {
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
    return new DisconnectResult(wasConnected);
  }
}
