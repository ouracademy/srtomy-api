package org.ouracademy.ovejafiel.event.command;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.springframework.stereotype.Component;

/**
 * Tells people when what they signed up for goes away: an event is deleted, or the way a sub-event
 * is entered changes and their own sign-up stops counting. Whoever holds a place is told once, even
 * when several of the events are theirs (a mega event and its sub-events).
 */
@Component
@RequiredArgsConstructor
public class RegistrationNotices {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EffectiveRegistration effectiveRegistration;
  private final SendEmailCommandHandler sendEmailCommandHandler;

  /**
   * Emails the people whose place is in one of {@code deleted} (their own, or the mega event's for
   * a sub-event that inherits it). Events already over are not announced. Returns how many people
   * were told.
   */
  public int cancelled(Collection<Event> deleted) {
    var now = ZonedDateTime.now();
    var titles = new LinkedHashMap<EventId, List<String>>();
    for (var event : deleted) {
      if (!event.getEnd().isAfter(now)) {
        continue;
      }
      effectiveRegistration
          .governing(event)
          .ifPresent(
              governing ->
                  titles
                      .computeIfAbsent(governing.getId(), id -> new java.util.ArrayList<>())
                      .add(event.getTitle()));
    }
    if (titles.isEmpty()) {
      return 0;
    }
    var perPerson = new LinkedHashMap<String, LinkedHashSet<String>>();
    var names = new LinkedHashMap<String, String>();
    for (var registration :
        eventRegistrationRepository.findByEventIdInAndStatusNot(
            List.copyOf(titles.keySet()), RegistrationStatus.CANCELLED)) {
      var email = registration.getEmail().trim().toLowerCase();
      perPerson
          .computeIfAbsent(email, key -> new LinkedHashSet<>())
          .addAll(titles.get(registration.getEvent().getId()));
      names.putIfAbsent(email, registration.getFirstName());
    }
    perPerson.forEach(
        (email, theirs) -> {
          var text = new StringBuilder("Hola " + names.get(email) + ",\n\n");
          text.append(
              theirs.size() == 1
                  ? "Se canceló un evento al que te inscribiste:\n\n"
                  : "Se cancelaron eventos a los que te inscribiste:\n\n");
          theirs.forEach(title -> text.append("• ").append(title).append("\n"));
          text.append("\nTu inscripción ya no vale para ")
              .append(theirs.size() == 1 ? "él" : "ellos")
              .append(". Disculpa las molestias.\n");
          sendEmailCommandHandler.handle(
              new SendEmailCommand(
                  "EVENT_CANCELLED",
                  email,
                  "Se canceló «" + theirs.iterator().next() + "»",
                  text.toString()));
        });
    return perPerson.size();
  }

  /**
   * The sub-event stops using its own sign-up (it now inherits or is open): its own sign-ups are
   * cancelled and their holders told how to enter now. Returns how many people were told.
   */
  public int ownCancelled(Event subEvent, String howToEnter) {
    var told = 0;
    for (var registration :
        eventRegistrationRepository.findByEventIdAndStatusNotOrderByCreateDate(
            subEvent.getId(), RegistrationStatus.CANCELLED)) {
      registration.cancel();
      sendEmailCommandHandler.handle(
          new SendEmailCommand(
              "REGISTRATION_CHANGED",
              registration.getEmail().trim().toLowerCase(),
              "Cambió la inscripción de «" + subEvent.getTitle() + "»",
              "Hola "
                  + registration.getFirstName()
                  + ",\n\nLa inscripción propia de «"
                  + subEvent.getTitle()
                  + "» ya no se usa, así que se canceló la tuya. "
                  + howToEnter
                  + "\n"));
      told++;
    }
    return told;
  }
}
