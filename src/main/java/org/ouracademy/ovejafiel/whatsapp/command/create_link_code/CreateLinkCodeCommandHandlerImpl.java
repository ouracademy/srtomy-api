package org.ouracademy.ovejafiel.whatsapp.command.create_link_code;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.LinkCodes;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateLinkCodeCommandHandlerImpl implements CreateLinkCodeCommandHandler {

  static final Duration LIFETIME = Duration.ofMinutes(10);

  private final FindUserBySubjectQueryHandler findUser;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final FindLinkedPersonQueryHandler findLinkedPerson;
  private final WhatsAppConnectionRepository connections;
  private final WhatsAppLinkRepository links;

  @Override
  @Transactional
  public CreateLinkCodeResult handle(CreateLinkCodeCommand command) {
    var user =
        findUser
            .handle(new FindUserBySubjectQuery(command.subject()))
            .orElseThrow(() -> new NoSuchElementException("User not found"));
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    if (findLinkedPerson.handle(new FindLinkedPersonQuery(user.id(), churchId)).isEmpty()) {
      throw new DomainValidationException(
          "Primero vincula tu cuenta con tu persona en esta iglesia");
    }
    var connection = connections.findById(churchId).filter(c -> c.isConnected());
    if (connection.isEmpty()) {
      throw new DomainValidationException("La iglesia todavía no conectó su WhatsApp");
    }

    var code = LinkCodes.generate();
    var expiresAt = LocalDateTime.now().plus(LIFETIME);
    links.saveCode(user.id().value(), churchId.value(), LinkCodes.hash(code), expiresAt);
    return new CreateLinkCodeResult(code, expiresAt, connection.get().getDisplayNumber());
  }
}
