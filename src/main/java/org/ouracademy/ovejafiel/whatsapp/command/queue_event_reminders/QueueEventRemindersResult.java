package org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders;

/**
 * @param queued reminders put in the outbox
 * @param skipped sign-ups left out: church without WhatsApp, the person asked to stop, or already
 *     reminded
 */
public record QueueEventRemindersResult(int queued, int skipped) {}
