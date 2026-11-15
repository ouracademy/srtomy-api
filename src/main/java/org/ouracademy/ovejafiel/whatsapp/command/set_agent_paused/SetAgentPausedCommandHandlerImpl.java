package org.ouracademy.ovejafiel.whatsapp.command.set_agent_paused;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stops (or starts again) the answers of the assistant without touching the connection: the number
 * stays connected, the reminders keep going out and the commands STOP, ALTA and VINCULAR still
 * work. What stops is the assistant answering questions, so the church can answer by hand.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class SetAgentPausedCommandHandlerImpl implements SetAgentPausedCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final FindChurchOfOrganizationQueryHandler findChurch;

  @Override
  @Transactional
  public SetAgentPausedResult handle(SetAgentPausedCommand command) {
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var connection =
        connections
            .findById(churchId)
            .orElseThrow(() -> new NoSuchElementException("The church has no WhatsApp"));
    if (!connection.isConnected()) {
      throw new DomainValidationException("El WhatsApp de la iglesia no está conectado");
    }
    if (command.paused()) {
      connection.pauseAgent();
    } else {
      connection.resumeAgent();
    }
    connections.save(connection);
    log.info(
        "WhatsApp assistant of church {} {}",
        churchId.value(),
        command.paused() ? "paused" : "resumed");
    return new SetAgentPausedResult(connection.isAgentPaused());
  }
}
