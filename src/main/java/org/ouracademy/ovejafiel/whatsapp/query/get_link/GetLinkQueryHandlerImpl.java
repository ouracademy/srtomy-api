package org.ouracademy.ovejafiel.whatsapp.query.get_link;

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
class GetLinkQueryHandlerImpl implements GetLinkQueryHandler {

  private final FindUserBySubjectQueryHandler findUser;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final WhatsAppLinkRepository links;

  @Override
  @Transactional(readOnly = true)
  public GetLinkResult handle(GetLinkQuery query) {
    var user =
        findUser
            .handle(new FindUserBySubjectQuery(query.subject()))
            .orElseThrow(() -> new NoSuchElementException("User not found"));
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(query.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    return links
        .findIdentity(user.id().value(), churchId.value())
        .map(i -> new GetLinkResult(true, mask(i.phoneDigits()), i.verifiedAt()))
        .orElse(new GetLinkResult(false, null, null));
  }

  static String mask(String digits) {
    var tail = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    return "+" + "•".repeat(Math.max(0, digits.length() - tail.length())) + tail;
  }
}
