package org.ouracademy.ovejafiel.attendance.command.add_visitor;

import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Registers a new person as a visitor of the node the event belongs to, and marks them as present.
 * {@code subject} is the user who asks (null for a visitor who scans the QR without an account).
 * {@code markedBy} is the user who marked them, null when they did it themselves; {@code linkTo},
 * when given, is the account that becomes this person.
 */
public record AddVisitorCommand(
    String subject,
    EventId eventId,
    PersonInfo info,
    CheckInSource source,
    UserId markedBy,
    UserId linkTo) {}
