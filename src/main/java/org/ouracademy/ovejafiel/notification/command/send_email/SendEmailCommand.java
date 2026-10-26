package org.ouracademy.ovejafiel.notification.command.send_email;

public record SendEmailCommand(String to, String subject, String text) {}
