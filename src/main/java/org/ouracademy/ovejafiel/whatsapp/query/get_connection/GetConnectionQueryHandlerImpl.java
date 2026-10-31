package org.ouracademy.ovejafiel.whatsapp.query.get_connection;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionResult.State;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetConnectionQueryHandlerImpl implements GetConnectionQueryHandler {

  private final WhatsAppConnectionRepository connections;
  private final FindChurchOfOrganizationQueryHandler findChurch;

  @Override
  @Transactional(readOnly = true)
  public GetConnectionResult handle(GetConnectionQuery query) {
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(query.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var inherited = !churchId.equals(query.organizationId());
    return connections
        .findById(churchId)
        .map(
            c ->
                new GetConnectionResult(
                    State.valueOf(c.getStatus().name()),
                    c.getMode(),
                    c.getDisplayNumber(),
                    c.getConnectedAt(),
                    inherited,
                    churchId))
        .orElseGet(
            () ->
                new GetConnectionResult(
                    State.NOT_CONNECTED, null, null, null, inherited, churchId));
  }
}
