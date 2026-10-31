package org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.query.find_registrations_to_remind.FindRegistrationsToRemindQuery;
import org.ouracademy.ovejafiel.event.query.find_registrations_to_remind.FindRegistrationsToRemindQueryHandler;
import org.ouracademy.ovejafiel.event.query.find_registrations_to_remind.FindRegistrationsToRemindResult.Item;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.outbox.command.enqueue_message.EnqueueMessageCommand;
import org.ouracademy.ovejafiel.outbox.command.enqueue_message.EnqueueMessageCommandHandler;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppPayload;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class QueueEventRemindersCommandHandlerImpl implements QueueEventRemindersCommandHandler {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", Locale.forLanguageTag("es"));
  private static final String NO_PLACE = "por confirmar";

  private final FindRegistrationsToRemindQueryHandler findRegistrations;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final FindPeopleByPhoneQueryHandler findPeopleByPhone;
  private final WhatsAppConnectionRepository connections;
  private final WhatsAppReminderRepository reminders;
  private final EnqueueMessageCommandHandler enqueueMessage;

  @Override
  @Transactional
  public QueueEventRemindersResult handle(QueueEventRemindersCommand command) {
    var queued = 0;
    var skipped = 0;
    for (var item :
        findRegistrations
            .handle(new FindRegistrationsToRemindQuery(command.from(), command.to()))
            .registrations()) {
      if (queue(item)) {
        queued++;
      } else {
        skipped++;
      }
    }
    return new QueueEventRemindersResult(queued, skipped);
  }

  private boolean queue(Item item) {
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(item.organizationId()))
            .map(c -> c.churchId())
            .orElse(null);
    if (churchId == null || !hasWhatsApp(churchId) || askedToStop(item.phone(), churchId)) {
      return false;
    }
    // Last, so only a reminder that will really be queued is marked.
    if (!reminders.markQueued(item.registrationId(), LocalDateTime.now())) {
      return false;
    }
    enqueueMessage.handle(
        new EnqueueMessageCommand(
            OutboxChannel.WHATSAPP, "EVENT_REMINDER", item.phone(), payload(churchId, item)));
    return true;
  }

  private boolean hasWhatsApp(PartyId churchId) {
    return connections.findById(churchId).filter(c -> c.isConnected()).isPresent();
  }

  /** Someone of the church with that phone answered STOP: they signed up, but asked not to hear. */
  private boolean askedToStop(String phone, PartyId churchId) {
    return findPeopleByPhone.handle(new FindPeopleByPhoneQuery(phone, churchId)).people().stream()
        .anyMatch(person -> !person.whatsappConsent());
  }

  private static String payload(PartyId churchId, Item item) {
    var place = item.address() == null || item.address().isBlank() ? NO_PLACE : item.address();
    var variables = List.of(item.firstName(), item.eventTitle(), item.start().format(WHEN), place);
    try {
      return JSON.writeValueAsString(
          new WhatsAppPayload(churchId.value(), WhatsAppTemplate.EVENT_REMINDER, variables));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
