package org.ouracademy.srtomy.invitation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ouracademy.srtomy.invitation.InvitationFixtures.invitation;
import static org.ouracademy.srtomy.invitation.InvitationFixtures.organization;
import static org.ouracademy.srtomy.invitation.InvitationFixtures.user;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;

class InvitationTests {

  LocalDateTime now = LocalDateTime.of(2026, 10, 24, 12, 0);

  Invitation pending(LocalDateTime expiresAt) {
    return invitation(
        "Beto@Example.org",
        organization(PartyId.generate(), "Sede"),
        null,
        null,
        user("auth0|a", "a@x.org"),
        expiresAt);
  }

  @Test
  void aPendingInvitationPastItsDeadlineIsExpired() {
    assertThat(pending(now.plusDays(1)).effectiveStatus(now)).isEqualTo(InvitationStatus.PENDING);
    assertThat(pending(now).effectiveStatus(now)).isEqualTo(InvitationStatus.EXPIRED);
    assertThat(pending(now.minusDays(1)).effectiveStatus(now)).isEqualTo(InvitationStatus.EXPIRED);
  }

  @Test
  void onlyAPendingInvitationCanBeAccepted() {
    assertThatCode(() -> pending(now.plusDays(1)).requireAcceptable(now))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> pending(now.minusDays(1)).requireAcceptable(now))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("venció");

    var accepted = pending(now.plusDays(1));
    accepted.accept(user("auth0|b", "b@x.org"));
    assertThatThrownBy(() -> accepted.requireAcceptable(now))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("aceptada");

    var revoked = pending(now.plusDays(1));
    revoked.revoke();
    assertThatThrownBy(() -> revoked.requireAcceptable(now))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("cancelada");
  }

  @Test
  void acceptingRecordsWhoDidIt() {
    var invitation = pending(now.plusDays(1));
    var bob = user("auth0|b", "b@x.org");

    invitation.accept(bob);

    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(invitation.getAcceptedBy()).isSameAs(bob);
  }

  @Test
  void revokingAnAcceptedInvitationChangesNothing() {
    var invitation = pending(now.plusDays(1));
    invitation.accept(user("auth0|b", "b@x.org"));

    invitation.revoke();

    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
  }

  @Test
  void theEmailIsComparedIgnoringCaseAndSpaces() {
    var invitation = pending(now);

    assertThat(invitation.isFor("beto@example.org ")).isTrue();
    assertThat(invitation.isFor("other@example.org")).isFalse();
    assertThat(invitation.isFor(null)).isFalse();
  }
}
