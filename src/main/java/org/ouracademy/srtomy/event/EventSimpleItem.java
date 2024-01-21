package org.ouracademy.srtomy.event;

import java.time.ZonedDateTime;

public interface EventSimpleItem {
    Long getId();

    String getName();

    ZonedDateTime getStartTime();

    ZonedDateTime getEndTime();
}