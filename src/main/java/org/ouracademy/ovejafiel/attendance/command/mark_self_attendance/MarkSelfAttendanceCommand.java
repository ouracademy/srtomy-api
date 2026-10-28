package org.ouracademy.ovejafiel.attendance.command.mark_self_attendance;

/**
 * Someone scanned the QR of an event. {@code subject} is null if they are not signed in; {@code
 * name} is optional and only used when the account is not linked to a person of the church.
 */
public record MarkSelfAttendanceCommand(String token, String subject, String name) {}
