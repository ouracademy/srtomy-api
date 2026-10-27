package org.ouracademy.srtomy.event.query.list_my_events;

import java.time.ZonedDateTime;

/**
 * An event of the user's own history: {@code registered} when they signed up (by account, or by
 * their verified email), {@code attended} when the person linked to them was marked present.
 */
public record MyEventResult(
    String title,
    ZonedDateTime start,
    boolean isACourse,
    String organizationName,
    boolean registered,
    boolean attended,
    String slug) {}
