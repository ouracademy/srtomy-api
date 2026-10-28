package org.ouracademy.ovejafiel.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class EventRegistrationTests {

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void aTicketCleansItsNameAndDefaultsTheCurrency() {
    var ticket = new TicketType(event, 0);

    ticket.describe("  VIP ", 2500, " ", 10, null, null);

    assertThat(ticket.getName()).isEqualTo("VIP");
    assertThat(ticket.getCurrency()).isEqualTo("PEN");
    assertThat(ticket.getPriceCents()).isEqualTo(2500);
  }

  @Test
  void aTicketMustMakeSense() {
    var ticket = new TicketType(event, 0);
    var now = ZonedDateTime.now();

    assertThatThrownBy(() -> ticket.describe(null, 0, "PEN", 1, null, null))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> ticket.describe("A", -1, "PEN", 1, null, null))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> ticket.describe("A", 0, "PEN", 0, null, null))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> ticket.describe("A", 0, "PEN", 1, now, now))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aTicketIsOnSaleInsideItsWindowOnly() {
    var ticket = new TicketType(event, 0);
    var now = ZonedDateTime.parse("2026-03-01T12:00:00Z");

    ticket.describe("A", 0, "PEN", null, null, null);
    assertThat(ticket.isOnSale(now)).isTrue();
    ticket.describe("A", 0, "PEN", null, now.plusDays(1), null);
    assertThat(ticket.isOnSale(now)).isFalse();
    ticket.describe("A", 0, "PEN", null, null, now.minusDays(1));
    assertThat(ticket.isOnSale(now)).isFalse();
    ticket.describe("A", 0, "PEN", null, now.minusDays(1), now.plusDays(1));
    assertThat(ticket.isOnSale(now)).isTrue();
  }

  @Test
  void aNewSetupIsOpenAndCanBeClosed() {
    var setup = new EventRegistrationSetup(event);

    assertThat(setup.isOpen()).isTrue();
    assertThat(setup.isRequiresAccount()).isFalse();
    setup.configure(true);
    setup.setOpen(false);
    assertThat(setup.isRequiresAccount()).isTrue();
    assertThat(setup.isOpen()).isFalse();
  }

  @Test
  void aNewSignUpHasDifferentCancelAndTicketCodes() {
    var first = registration(event, ticket(event, "A", 1), "a@x.org", RegistrationStatus.CONFIRMED);
    var second =
        registration(event, ticket(event, "A", 1), "b@x.org", RegistrationStatus.CONFIRMED);

    assertThat(first.getCancelCode()).hasSize(16).isNotEqualTo(first.getTicketCode());
    assertThat(first.getTicketCode()).isNotEqualTo(second.getTicketCode());
  }

  @Test
  void checkingInIsIdempotentAndKeepsTheFirstSource() {
    var registration =
        registration(event, ticket(event, "A", 1), "a@x.org", RegistrationStatus.CONFIRMED);

    registration.checkIn(CheckInSource.QR_TICKET);
    var firstTime = registration.getCheckedInAt();
    registration.checkIn(CheckInSource.MANUAL);

    assertThat(registration.getCheckedInAt()).isEqualTo(firstTime);
    assertThat(registration.getCheckInSource()).isEqualTo(CheckInSource.QR_TICKET);
  }

  @Test
  void onlyAConfirmedSignUpChecksIn() {
    var cancelled =
        registration(event, ticket(event, "A", 1), "a@x.org", RegistrationStatus.CANCELLED);

    assertThatThrownBy(() -> cancelled.checkIn(CheckInSource.MANUAL))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void cancellingClearsTheCheckIn() {
    var registration =
        registration(event, ticket(event, "A", 1), "a@x.org", RegistrationStatus.CONFIRMED);
    registration.checkIn(CheckInSource.MANUAL);

    registration.cancel();

    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    assertThat(registration.isCheckedIn()).isFalse();
    assertThat(registration.getStatus().isActive()).isFalse();
  }
}
