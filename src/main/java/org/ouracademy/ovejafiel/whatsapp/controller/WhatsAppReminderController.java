package org.ouracademy.ovejafiel.whatsapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders.QueueEventRemindersCommand;
import org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders.QueueEventRemindersCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders.QueueEventRemindersResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by Cloud Scheduler once a day, never by the web app. The window is the day after the next
 * twelve hours, so two daily runs never cover the same event; a sign-up is reminded once anyway.
 */
@RestController
@AllArgsConstructor
@Hidden
public class WhatsAppReminderController {

  static final Duration FROM_NOW = Duration.ofHours(12);
  static final Duration WINDOW = Duration.ofHours(24);

  QueueEventRemindersCommandHandler queueEventRemindersCommandHandler;

  @PostMapping("internal/events/whatsapp-reminders")
  public QueueEventRemindersResult remind() {
    var from = Instant.now().plus(FROM_NOW);
    return queueEventRemindersCommandHandler.handle(
        new QueueEventRemindersCommand(from, from.plus(WINDOW)));
  }
}
