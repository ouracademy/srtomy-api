package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.list_my_events.MyEventResult;

/**
 * An event or course where the user has a place or was present; open it at {@code /e/{slug}}. The
 * registration fields are null without a sign-up; {@code cancelCode} and {@code ticketCode} are the
 * user's own.
 */
public record MyEventResponse(
    String title,
    ZonedDateTime start,
    boolean isACourse,
    String organizationName,
    boolean registered,
    boolean attended,
    String slug,
    UUID registrationId,
    String ticketName,
    RegistrationStatus registrationStatus,
    String cancelCode,
    String ticketCode,
    String parentTitle) {

  public static MyEventResponse from(MyEventResult result) {
    return new MyEventResponse(
        result.title(),
        result.start(),
        result.isACourse(),
        result.organizationName(),
        result.registered(),
        result.attended(),
        result.slug(),
        result.registrationId() == null ? null : result.registrationId().value(),
        result.ticketName(),
        result.registrationStatus(),
        result.cancelCode(),
        result.ticketCode(),
        result.parentTitle());
  }
}
