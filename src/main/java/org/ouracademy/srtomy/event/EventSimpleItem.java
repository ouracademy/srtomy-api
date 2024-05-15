package org.ouracademy.srtomy.event;

import java.time.ZonedDateTime;

public interface EventSimpleItem {
    Long getId();

    String getTitle();

    ZonedDateTime getStart();

    ZonedDateTime getEnd();
}