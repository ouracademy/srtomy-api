package org.ouracademy.ovejafiel.event.command.extend_event_series;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventSeriesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ExtendEventSeriesCommandHandlerImpl implements ExtendEventSeriesCommandHandler {

  private final EventSeriesRepository eventSeriesRepository;
  private final EventRepository eventRepository;

  @Override
  @Transactional
  public ExtendEventSeriesResult handle(ExtendEventSeriesCommand command) {
    // Series whose occurrences end less than 6 months from now get a new year of them.
    var threshold = command.today().plusMonths(EventSeries.MATERIALIZED_MONTHS / 2);
    var created =
        eventSeriesRepository
            .findByFullyMaterializedFalseAndMaterializedUntilBefore(threshold)
            .stream()
            .mapToInt(series -> extend(series, command))
            .sum();
    return new ExtendEventSeriesResult(created);
  }

  private int extend(EventSeries series, ExtendEventSeriesCommand command) {
    var occurrences = series.materialize(series.horizonFrom(command.today()));
    eventRepository.saveAll(occurrences);
    return occurrences.size();
  }
}
