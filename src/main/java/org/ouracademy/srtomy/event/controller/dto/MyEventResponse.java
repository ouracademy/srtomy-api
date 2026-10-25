package org.ouracademy.srtomy.event.controller.dto;

import java.time.ZonedDateTime;
import org.ouracademy.srtomy.event.query.list_my_events.MyEventResult;

/** An event or course the user signed up for or attended; open it at {@code /e/{slug}}. */
public record MyEventResponse(
    String title,
    ZonedDateTime start,
    boolean isACourse,
    String organizationName,
    boolean registered,
    boolean attended,
    String slug) {

  public static MyEventResponse from(MyEventResult result) {
    return new MyEventResponse(
        result.title(),
        result.start(),
        result.isACourse(),
        result.organizationName(),
        result.registered(),
        result.attended(),
        result.slug());
  }
}
