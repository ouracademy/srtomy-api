package org.ouracademy.srtomy.event.command.extend_event_series;

import java.time.LocalDate;

/** Creates the occurrences still missing, a year ahead of {@code today}, of every open series. */
public record ExtendEventSeriesCommand(LocalDate today) {}
