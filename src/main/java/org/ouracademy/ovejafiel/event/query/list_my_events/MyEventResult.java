package org.ouracademy.ovejafiel.event.query.list_my_events;

import java.time.ZonedDateTime;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;

/**
 * An event of the user's own history. {@code registered} when they hold a seat or wait for one (by
 * account, or by their verified email), {@code attended} when they were marked present (the person
 * linked to them, or their ticket). The registration fields are null when they have none; {@code
 * cancelCode} and {@code ticketCode} are theirs alone, to cancel and to show the ticket QR. A
 * sub-event that inherits the ticket of its mega event carries that sign-up (cancelling it cancels
 * the whole sign-up) and {@code parentTitle}, the mega event's name.
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
    String ticketCode,
    String parentTitle) {}
