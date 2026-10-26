package org.ouracademy.srtomy.event.command.extend_event_series;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.event.domain.EventSeries;
import org.ouracademy.srtomy.event.domain.Recurrence;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.event.repository.EventSeriesRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class ExtendEventSeriesCommandHandlerImplTests {

  @Mock EventSeriesRepository eventSeriesRepository;
  @Mock EventRepository eventRepository;

  @Test
  void createsTheMissingOccurrencesOfTheSeriesThatRunOutSoon() {
    var series =
        new EventSeries(
            "Oración",
            LocalDateTime.of(2026, 1, 1, 6, 0),
            Duration.ofHours(1),
            ZoneId.of("UTC"),
            Recurrence.of(
                Recurrence.Frequency.DAILY, 1, null, null, Recurrence.EndType.NEVER, null, null));
    series.setOrganization(organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH));
    series.materialize(LocalDate.of(2026, 1, 10));
    var today = LocalDate.of(2026, 1, 8);
    when(eventSeriesRepository.findByFullyMaterializedFalseAndMaterializedUntilBefore(
            LocalDate.of(2026, 7, 8)))
        .thenReturn(List.of(series));

    var result =
        new ExtendEventSeriesCommandHandlerImpl(eventSeriesRepository, eventRepository)
            .handle(new ExtendEventSeriesCommand(today));

    // From Jan 11, 2026 to Jan 8, 2027.
    assertThat(result.createdEvents()).isEqualTo(363);
    assertThat(series.getMaterializedUntil()).isEqualTo(LocalDate.of(2027, 1, 8));
    verify(eventRepository).saveAll(any());
  }

  @Test
  void doesNothingWhenNoSeriesNeedsMoreOccurrences() {
    when(eventSeriesRepository.findByFullyMaterializedFalseAndMaterializedUntilBefore(
            LocalDate.of(2026, 7, 8)))
        .thenReturn(List.of());

    var result =
        new ExtendEventSeriesCommandHandlerImpl(eventSeriesRepository, eventRepository)
            .handle(new ExtendEventSeriesCommand(LocalDate.of(2026, 1, 8)));

    assertThat(result.createdEvents()).isZero();
  }
}
