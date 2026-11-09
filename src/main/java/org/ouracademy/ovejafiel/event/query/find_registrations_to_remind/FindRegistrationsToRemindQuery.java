package org.ouracademy.ovejafiel.event.query.find_registrations_to_remind;

import java.time.Instant;

/** The sign-ups whose event starts between {@code from} (included) and {@code to} (excluded). */
public record FindRegistrationsToRemindQuery(Instant from, Instant to) {}
