package org.ouracademy.ovejafiel.whatsapp.command.request_setup_link;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RequestSetupLinkCommandHandlerImpl implements RequestSetupLinkCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final WhatsAppGateway gateway;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final FindOrganizationByIdQueryHandler findOrganization;

  /**
   * The provider is called inside the transaction on purpose: nothing here must be repeated by a
   * queue (a link or a customer is harmless to repeat), and a failure saves nothing.
   */
  @Override
  @Transactional
  public RequestSetupLinkResult handle(RequestSetupLinkCommand command) {
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var connection =
        connections.findById(churchId).orElseGet(() -> newConnection(churchId, command));
    connection.startSetup(command.mode());
    connections.save(connection);

    var link =
        gateway.createSetupLink(
            connection.getKapsoCustomerId(), command.mode(), WhatsAppTemplate.LANGUAGE);
    return new RequestSetupLinkResult(link.url(), link.expiresAt());
  }

  private WhatsAppConnection newConnection(
      org.ouracademy.ovejafiel.party.domain.PartyId churchId, RequestSetupLinkCommand command) {
    var church =
        findOrganization
            .handle(new FindOrganizationByIdQuery(churchId))
            .orElseThrow(() -> new NoSuchElementException("Church not found"));
    var customerId = gateway.createCustomer(churchId.value().toString(), church.name());
    return new WhatsAppConnection(churchId, customerId, command.mode());
  }
}
