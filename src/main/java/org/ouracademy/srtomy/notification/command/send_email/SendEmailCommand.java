package org.ouracademy.srtomy.notification.command.send_email;

public record SendEmailCommand(String to, String subject, String text) {}
