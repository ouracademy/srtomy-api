package org.ouracademy.srtomy.event;

import java.time.LocalDateTime;

public interface EventSimpleItem {
    Long getId();

    String getName();

    LocalDateTime getStartTime();

    LocalDateTime getEndTime();
}