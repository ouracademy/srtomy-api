package org.ouracademy.ovejafiel.whatsapp.command.queue_event_reminders;

import java.time.Instant;

/** Remind everyone signed up for an event that starts in [{@code from}, {@code to}). */
public record QueueEventRemindersCommand(Instant from, Instant to) {}
