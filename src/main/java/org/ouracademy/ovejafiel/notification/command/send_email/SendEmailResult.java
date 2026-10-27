package org.ouracademy.ovejafiel.notification.command.send_email;

import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

/** The email is queued, not sent: ask the outbox for its status with this id. */
public record SendEmailResult(OutboxMessageId messageId) {}
