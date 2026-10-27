package org.ouracademy.ovejafiel.event.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.event.command.extend_event_series.ExtendEventSeriesCommand;
import org.ouracademy.ovejafiel.event.command.extend_event_series.ExtendEventSeriesCommandHandler;
import org.ouracademy.ovejafiel.event.command.extend_event_series.ExtendEventSeriesResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by Cloud Scheduler (never by the web app), once a day: keeps the series that never end (or
 * end far away) supplied with upcoming occurrences. Only the scheduler's service account may call
 * it, see {@code InternalSecurityConfig}.
 */
@RestController
@AllArgsConstructor
@Hidden
public class EventJobsController {

  ExtendEventSeriesCommandHandler extendEventSeriesCommandHandler;

  @PostMapping("internal/events/extend-series")
  public ExtendEventSeriesResult extendSeries() {
    return extendEventSeriesCommandHandler.handle(new ExtendEventSeriesCommand(LocalDate.now()));
  }
}
