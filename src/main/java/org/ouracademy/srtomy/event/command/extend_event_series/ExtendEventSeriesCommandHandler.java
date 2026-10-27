package org.ouracademy.srtomy.event.command.extend_event_series;

public interface ExtendEventSeriesCommandHandler {
  ExtendEventSeriesResult handle(ExtendEventSeriesCommand command);
}
