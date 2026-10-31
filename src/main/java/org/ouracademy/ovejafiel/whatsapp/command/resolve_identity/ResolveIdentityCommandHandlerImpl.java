package org.ouracademy.ovejafiel.whatsapp.command.resolve_identity;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQuery;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQueryHandler;
import org.ouracademy.ovejafiel.shared.DelegatedTokens;
import org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user.ResolveWhatsAppUserCommand;
import org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user.ResolveWhatsAppUserCommandHandler;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_user_by_id.FindUserByIdQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_id.FindUserByIdQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityResult.Level;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityResult.Status;
import org.ouracademy.ovejafiel.whatsapp.domain.LinkCodes;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppLinkRepository;
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
  private final WhatsAppLinkRepository links;
  private final FindUserByIdQueryHandler findUserById;
  private final FindLinkedPersonQueryHandler findLinkedPerson;
  private final FindPersonByIdQueryHandler findPerson;
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

    // A number the person proved from their account comes first: it is theirs, not a guess.
    var account = provedAccount(churchId, command.from(), church);
    if (account != null) {
      return account;
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
    var issued = tokenIssuer().issue(user.subject());
    return linked(issued, person.id().value(), church, person.name(), Level.MEMBER);
  }

  /** The account that proved this number in this church, if it still has a person there. */
  private ResolveIdentityResult provedAccount(
      PartyId churchId, String from, FindOrganizationResult church) {
    var userId = links.findUserByPhone(churchId.value(), LinkCodes.digitsOf(from));
    if (userId.isEmpty()) {
      return null;
    }
    var userKey = new UserId(userId.get());
    var user = findUserById.handle(new FindUserByIdQuery(userKey)).orElse(null);
    var person = findLinkedPerson.handle(new FindLinkedPersonQuery(userKey, churchId)).orElse(null);
    if (user == null || person == null) {
      return null; // the account or its person is gone: the proof no longer means anything
    }
    var name = findPerson.handle(new FindPersonByIdQuery(person.personId())).map(p -> p.name());
    var issued = tokenIssuer().issue(user.subject());
    return linked(issued, person.personId().value(), church, name.orElse(null), Level.ACCOUNT);
  }

  private DelegatedTokens tokenIssuer() {
    var tokens = delegatedTokens.getIfAvailable();
    if (tokens == null) {
      throw new IllegalStateException("delegation.private-key is not configured");
    }
    return tokens;
  }

  private static ResolveIdentityResult linked(
      DelegatedTokens.Issued issued,
      java.util.UUID personId,
      FindOrganizationResult church,
      String name,
      Level level) {
    return new ResolveIdentityResult(
        Status.LINKED,
        issued.token(),
        issued.expiresAt(),
        personId,
        church.id().value(),
        church.name(),
        church.slug(),
        name,
        level);
  }

  private static ResolveIdentityResult known(Status status, FindOrganizationResult church) {
    return ResolveIdentityResult.ofChurch(
        status, church.id().value(), church.name(), church.slug());
  }
}
