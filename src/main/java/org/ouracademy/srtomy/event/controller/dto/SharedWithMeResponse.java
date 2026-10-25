package org.ouracademy.srtomy.event.controller.dto;

import java.time.ZonedDateTime;
import org.ouracademy.srtomy.event.domain.ShareRole;
import org.ouracademy.srtomy.event.query.list_events_shared_with.SharedWithMeResult;

/** An event someone shared with the user; open it at {@code /e/{slug}}. */
public record SharedWithMeResponse(
    String title,
    ZonedDateTime start,
    boolean isACourse,
    String organizationName,
    ShareRole role,
    String slug) {

  public static SharedWithMeResponse from(SharedWithMeResult result) {
    return new SharedWithMeResponse(
        result.title(),
        result.start(),
        result.isACourse(),
        result.organizationName(),
        result.role(),
        result.slug());
  }
}
