package org.ouracademy.srtomy.notification.command.send_email;

import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

/** The email is queued, not sent: ask the outbox for its status with this id. */
public record SendEmailResult(OutboxMessageId messageId) {}
