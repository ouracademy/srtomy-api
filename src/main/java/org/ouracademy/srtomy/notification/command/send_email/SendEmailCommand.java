package org.ouracademy.srtomy.notification.command.send_email;

/**
 * {@code type}: what the email is about (INVITATION, EVENT_SHARE...), for whoever reads the outbox.
 */
public record SendEmailCommand(String type, String to, String subject, String text) {}
