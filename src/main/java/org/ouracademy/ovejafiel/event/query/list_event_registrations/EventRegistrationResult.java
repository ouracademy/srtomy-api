package org.ouracademy.ovejafiel.event.query.list_event_registrations;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;

/**
 * Someone with a seat or waiting for one; {@code hasAccount} when they were signed in. {@code
 * checkedInAt} is when they were marked as present, if they were.
 */
public record EventRegistrationResult(
    EventRegistrationId id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    boolean hasAccount,
    String ticketName,
    RegistrationStatus status,
    LocalDateTime registeredAt,
    LocalDateTime checkedInAt,
    CheckInSource checkInSource) {}
