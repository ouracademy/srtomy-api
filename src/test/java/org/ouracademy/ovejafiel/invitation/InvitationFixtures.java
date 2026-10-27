package org.ouracademy.ovejafiel.invitation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationTokens;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.user.domain.AppUser;

/** Test helpers for the invitation module. */
public final class InvitationFixtures {

  private InvitationFixtures() {}

  public static Organization organization(PartyId id, String name) {
    var organization = mock(Organization.class);
    when(organization.getId()).thenReturn(id);
    when(organization.getName()).thenReturn(name);
    return organization;
  }

  public static AppUser user(String subject, String email) {
    var user = new AppUser(subject);
    user.updateContact(email, true);
    return user;
  }

  /** A pending invitation to {@code email} for a new person, valid until {@code expiresAt}. */
  public static Invitation invitation(
      String email,
      Organization organization,
      AccountabilityType position,
      Person person,
      AppUser inviter,
      LocalDateTime expiresAt) {
    return new Invitation(
        email,
        organization,
        position,
        person,
        person == null ? "Beto" : null,
        person == null ? "Pérez" : null,
        InvitationTokens.hash("token"),
        expiresAt,
        inviter);
  }
}
