package org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders;

/**
 * Puts a WhatsApp reminder in the outbox for each person signed up for an event that is about to
 * happen. Meant to run once a day; running it again does not repeat a reminder.
 */
public interface QueueEventRemindersCommandHandler {
  QueueEventRemindersResult handle(QueueEventRemindersCommand command);
}
