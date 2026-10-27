package org.ouracademy.srtomy.notification.command.send_email;

/**
 * The payload of an outbox message of the EMAIL channel (as JSON); the recipient is its own column.
 */
public record EmailContent(String subject, String text) {}
