package org.ouracademy.srtomy.event.query.list_event_registrations;

import java.time.LocalDateTime;
import org.ouracademy.srtomy.event.domain.EventRegistrationId;

/** Someone signed up for the event; {@code hasAccount} when they were signed in. */
public record EventRegistrationResult(
    EventRegistrationId id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    boolean hasAccount,
    LocalDateTime registeredAt) {}
