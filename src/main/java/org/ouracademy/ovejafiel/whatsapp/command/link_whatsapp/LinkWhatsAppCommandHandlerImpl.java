package org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp.LinkWhatsAppResult.Status;
import org.ouracademy.ovejafiel.whatsapp.domain.LinkCodes;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class LinkWhatsAppCommandHandlerImpl implements LinkWhatsAppCommandHandler {

  static final int MAX_FAILURES = 5;
  static final Duration WINDOW = Duration.ofHours(1);

  private final WhatsAppConnectionRepository connections;
  private final WhatsAppLinkRepository links;
  private final FindOrganizationByIdQueryHandler findOrganization;

  /** A wrong code is answered, never thrown: the count of wrong tries must be committed. */
  @Override
  @Transactional
  public LinkWhatsAppResult handle(LinkWhatsAppCommand command) {
    var connection =
        connections.findByPhoneNumberId(command.phoneNumberId()).filter(c -> c.isConnected());
    if (connection.isEmpty()) {
      return LinkWhatsAppResult.of(Status.CHURCH_NOT_CONNECTED);
    }
    var churchId = connection.get().getOrganizationId().value();
    var phone = LinkCodes.digitsOf(command.from());
    var now = LocalDateTime.now();

    // Before looking at the code: a guesser must not learn whether a try was right.
    if (links.countFailuresSince(phone, now.minus(WINDOW)) >= MAX_FAILURES) {
      return LinkWhatsAppResult.of(Status.RATE_LIMITED);
    }
    var code =
        LinkCodes.looksLikeACode(command.code())
            ? links.findValidCode(LinkCodes.hash(command.code()), churchId, now)
            : java.util.Optional.<WhatsAppLinkRepository.Code>empty();
    if (code.isEmpty()) {
      links.recordFailure(phone, now);
      return LinkWhatsAppResult.of(Status.INVALID_CODE);
    }

    links.saveIdentity(code.get().userId(), churchId, phone, now);
    links.deleteCode(code.get().userId());
    var name =
        findOrganization
            .handle(new FindOrganizationByIdQuery(connection.get().getOrganizationId()))
            .map(church -> church.name())
            .orElse(null);
    return new LinkWhatsAppResult(Status.LINKED, name);
  }
}
