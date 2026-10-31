package org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UnlinkWhatsAppCommandHandlerImpl implements UnlinkWhatsAppCommandHandler {

  private final FindUserBySubjectQueryHandler findUser;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final WhatsAppLinkRepository links;

  @Override
  @Transactional
  public UnlinkWhatsAppResult handle(UnlinkWhatsAppCommand command) {
    var user =
        findUser
            .handle(new FindUserBySubjectQuery(command.subject()))
            .orElseThrow(() -> new NoSuchElementException("User not found"));
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    return new UnlinkWhatsAppResult(links.deleteIdentity(user.id().value(), churchId.value()));
  }
}
