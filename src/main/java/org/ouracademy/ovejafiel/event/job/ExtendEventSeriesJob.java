package org.ouracademy.ovejafiel.event.job;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.event.command.extend_event_series.ExtendEventSeriesCommand;
import org.ouracademy.ovejafiel.event.command.extend_event_series.ExtendEventSeriesCommandHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Keeps the series that never end (or end far away) supplied with upcoming occurrences. */
@Slf4j
@Component
@RequiredArgsConstructor
class ExtendEventSeriesJob {

  private final ExtendEventSeriesCommandHandler extendEventSeriesCommandHandler;

  @Scheduled(cron = "${ovejafiel.events.extend-series-cron:0 30 3 * * *}", zone = "UTC")
  void run() {
    var result =
        extendEventSeriesCommandHandler.handle(new ExtendEventSeriesCommand(LocalDate.now()));
    log.info("Extended event series: {} new events", result.createdEvents());
  }
}
