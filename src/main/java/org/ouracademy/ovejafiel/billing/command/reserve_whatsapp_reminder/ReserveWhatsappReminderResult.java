package org.ouracademy.ovejafiel.billing.command.reserve_whatsapp_reminder;

/**
 * @param allowed false when the plan has no more reminders this month: the caller does not send it
 * @param used reminders of the month, this one included when allowed
 * @param limit reminders a month of the plan (0 when it has none)
 */
public record ReserveWhatsappReminderResult(boolean allowed, int used, int limit) {}
