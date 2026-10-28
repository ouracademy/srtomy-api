package org.ouracademy.ovejafiel.event.query.list_my_events;

import java.time.ZonedDateTime;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;

/**
 * An event of the user's own history. {@code registered} when they hold a seat or wait for one (by
 * account, or by their verified email), {@code attended} when they were marked present (the person
 * linked to them, or their ticket). The registration fields are null when they have none; {@code
 * cancelCode} and {@code ticketCode} are theirs alone, to cancel and to show the ticket QR.
 */
public record MyEventResult(
    String title,
    ZonedDateTime start,
    boolean isACourse,
    String organizationName,
    boolean registered,
    boolean attended,
    String slug,
    EventRegistrationId registrationId,
    String ticketName,
    RegistrationStatus registrationStatus,
    String cancelCode,
    String ticketCode) {}
