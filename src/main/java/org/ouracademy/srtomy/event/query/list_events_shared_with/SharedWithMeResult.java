package org.ouracademy.srtomy.event.query.list_events_shared_with;

import java.time.ZonedDateTime;
import org.ouracademy.srtomy.event.domain.ShareRole;

/** An event someone shared with the user, with the way to open it. */
public record SharedWithMeResult(
    String title,
    ZonedDateTime start,
    boolean isACourse,
    String organizationName,
    ShareRole role,
    String slug) {}
