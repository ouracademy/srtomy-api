package org.ouracademy.ovejafiel.whatsapp.command.resolve_identity;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQueryHandler;
import org.ouracademy.ovejafiel.shared.DelegatedTokens;
import org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user.ResolveWhatsAppUserCommand;
import org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user.ResolveWhatsAppUserCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityResult.Status;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ResolveIdentityCommandHandlerImpl implements ResolveIdentityCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final FindPeopleByPhoneQueryHandler findPeopleByPhone;
  private final ResolveWhatsAppUserCommandHandler resolveWhatsAppUser;
  private final FindOrganizationByIdQueryHandler findOrganization;
  private final ObjectProvider<DelegatedTokens> delegatedTokens;

  @Override
  @Transactional
  public ResolveIdentityResult handle(ResolveIdentityCommand command) {
    var connection =
        connections.findByPhoneNumberId(command.phoneNumberId()).filter(c -> c.isConnected());
    if (connection.isEmpty()) {
      return ResolveIdentityResult.of(Status.CHURCH_NOT_CONNECTED);
    }
    var churchId = connection.get().getOrganizationId();
    var church = findOrganization.handle(new FindOrganizationByIdQuery(churchId)).orElse(null);
    if (church == null) {
      return ResolveIdentityResult.of(Status.CHURCH_NOT_CONNECTED);
    }

    var people = findPeopleByPhone.handle(new FindPeopleByPhoneQuery(command.from(), churchId));
    if (people.people().isEmpty()) {
      return known(Status.UNKNOWN, church);
    }
    if (people.people().size() > 1) {
      return known(Status.AMBIGUOUS, church);
    }
    var person = people.people().getFirst();

    var user = resolveWhatsAppUser.handle(new ResolveWhatsAppUserCommand(person.id(), churchId));
    if (user.realAccount()) {
      return known(Status.NEEDS_VERIFICATION, church);
    }
    var tokens = delegatedTokens.getIfAvailable();
    if (tokens == null) {
      throw new IllegalStateException("delegation.private-key is not configured");
    }
    var issued = tokens.issue(user.subject());
    return new ResolveIdentityResult(
        Status.LINKED,
        issued.token(),
        issued.expiresAt(),
        person.id().value(),
        churchId.value(),
        church.name(),
        church.slug(),
        person.name());
  }

  private static ResolveIdentityResult known(Status status, FindOrganizationResult church) {
    return ResolveIdentityResult.ofChurch(
        status, church.id().value(), church.name(), church.slug());
  }
}
