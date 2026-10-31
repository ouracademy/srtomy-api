package org.ouracademy.ovejafiel.whatsapp.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.query.find_registrations_to_remind.FindRegistrationsToRemindQueryHandler;
import org.ouracademy.ovejafiel.event.query.find_registrations_to_remind.FindRegistrationsToRemindResult;
import org.ouracademy.ovejafiel.event.query.find_registrations_to_remind.FindRegistrationsToRemindResult.Item;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.outbox.command.enqueue_message.EnqueueMessageCommand;
import org.ouracademy.ovejafiel.outbox.command.enqueue_message.EnqueueMessageCommandHandler;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneResult;
import org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders.QueueEventRemindersCommand;
import org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders.QueueEventRemindersCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppReminderRepository;

/** The reminder of an event: who gets it, who does not, and what it says. */
class QueueEventRemindersTests {

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();
  UUID registration = UUID.randomUUID();

  FindRegistrationsToRemindQueryHandler findRegistrations =
      mock(FindRegistrationsToRemindQueryHandler.class);
  FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  FindPeopleByPhoneQueryHandler findPeople = mock(FindPeopleByPhoneQueryHandler.class);
  WhatsAppConnectionRepository connections = mock(WhatsAppConnectionRepository.class);
  WhatsAppReminderRepository reminders = mock(WhatsAppReminderRepository.class);
  EnqueueMessageCommandHandler enqueue = mock(EnqueueMessageCommandHandler.class);

  QueueEventRemindersCommandHandler handler() throws Exception {
    var constructor =
        Class.forName(
                "org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders."
                    + "QueueEventRemindersCommandHandlerImpl")
            .getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    return (QueueEventRemindersCommandHandler)
        constructor.newInstance(
            findRegistrations, findChurch, findPeople, connections, reminders, enqueue);
  }

  QueueEventRemindersCommand window() {
    return new QueueEventRemindersCommand(Instant.now(), Instant.now().plusSeconds(86400));
  }

  Item item(String address) {
    return new Item(
        registration,
        "Ana",
        "999111222",
        "Culto de jóvenes",
        ZonedDateTime.of(2026, 11, 7, 19, 30, 0, 0, ZoneId.of("America/Lima")),
        address,
        node);
  }

  void scenario(Item item, boolean connected, boolean consent, boolean firstReminder) {
    when(findRegistrations.handle(any()))
        .thenReturn(new FindRegistrationsToRemindResult(List.of(item)));
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    if (connected) {
      var connection = new WhatsAppConnection(church, "cus", ConnectionMode.DEDICATED);
      connection.markConnected("pn", "+51", LocalDateTime.now());
      when(connections.findById(church)).thenReturn(Optional.of(connection));
    } else {
      when(connections.findById(church)).thenReturn(Optional.empty());
    }
    when(findPeople.handle(any()))
        .thenReturn(
            new FindPeopleByPhoneResult(
                List.of(new FindPeopleByPhoneResult.Match(PartyId.generate(), "Ana", consent))));
    when(reminders.markQueued(any(), any())).thenReturn(firstReminder);
  }

  @Test
  void aSignedUpPersonGetsOneReminderWithTheEventInSpanishAtTheirTime() throws Exception {
    scenario(item("Av. Perú 123"), true, true, true);

    var result = handler().handle(window());

    assertThat(result.queued()).isEqualTo(1);
    assertThat(result.skipped()).isZero();
    var sent = ArgumentCaptor.forClass(EnqueueMessageCommand.class);
    verify(enqueue).handle(sent.capture());
    assertThat(sent.getValue().channel()).isEqualTo(OutboxChannel.WHATSAPP);
    assertThat(sent.getValue().type()).isEqualTo("EVENT_REMINDER");
    assertThat(sent.getValue().recipient()).isEqualTo("999111222");
    assertThat(sent.getValue().payload())
        .contains(church.value().toString(), "EVENT_REMINDER")
        .contains("Ana", "Culto de jóvenes", "sábado 7 de noviembre, 19:30", "Av. Perú 123");
  }

  @Test
  void anEventWithoutAddressSaysThePlaceIsToBeConfirmed() throws Exception {
    scenario(item(null), true, true, true);

    handler().handle(window());

    var sent = ArgumentCaptor.forClass(EnqueueMessageCommand.class);
    verify(enqueue).handle(sent.capture());
    assertThat(sent.getValue().payload()).contains("por confirmar");
  }

  @Test
  void aChurchWithoutWhatsappGetsNothingAndNothingIsMarked() throws Exception {
    scenario(item("x"), false, true, true);

    var result = handler().handle(window());

    assertThat(result.queued()).isZero();
    assertThat(result.skipped()).isEqualTo(1);
    verifyNoInteractions(enqueue);
    verify(reminders, never()).markQueued(any(), any());
  }

  @Test
  void whoAskedToStopIsNotRemindedEvenIfTheySignedUp() throws Exception {
    scenario(item("x"), true, false, true);

    var result = handler().handle(window());

    assertThat(result.queued()).isZero();
    assertThat(result.skipped()).isEqualTo(1);
    verifyNoInteractions(enqueue);
    verify(reminders, never()).markQueued(any(), any());
  }

  @Test
  void aSecondRunDoesNotRepeatAReminder() throws Exception {
    scenario(item("x"), true, true, false);

    var result = handler().handle(window());

    assertThat(result.queued()).isZero();
    assertThat(result.skipped()).isEqualTo(1);
    verifyNoInteractions(enqueue);
  }

  @Test
  void nobodyToRemindIsAnEmptyRun() throws Exception {
    when(findRegistrations.handle(any()))
        .thenReturn(new FindRegistrationsToRemindResult(List.of()));

    var result = handler().handle(window());

    assertThat(result.queued()).isZero();
    assertThat(result.skipped()).isZero();
  }
}
