package org.ouracademy.ovejafiel.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventSeries;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class EventSeriesTests {

  Organization church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);

  private EventSeries daily() {
    var series =
        new EventSeries(
            "Oración",
            LocalDateTime.of(2026, 1, 5, 6, 0),
            Duration.ofMinutes(30),
            ZoneId.of("America/Lima"),
            Recurrence.of(
                Recurrence.Frequency.DAILY, 1, null, null, Recurrence.EndType.NEVER, null, null));
    series.setOrganization(church);
    return series;
  }

  @Test
  void materializeCreatesTheOccurrencesUpToTheHorizonInUtc() {
    var series = eventSeries(church);

    var created = series.materialize(LocalDate.of(2026, 1, 31));

    assertThat(created).hasSize(4);
    assertThat(created.getFirst().getStart().toString()).isEqualTo("2026-01-05T15:00Z[UTC]");
    assertThat(created.getFirst().getEnd().toString()).isEqualTo("2026-01-05T17:00Z[UTC]");
    assertThat(created.getFirst().getSeries()).isSameAs(series);
    assertThat(created.getFirst().getTitle()).isEqualTo("Servicio");
    assertThat(created.getFirst().getOrganization()).isSameAs(church);
    assertThat(series.isFullyMaterialized()).isTrue();
  }

  @Test
  void anOccurrenceKeepsTheLocalHourOfTheSeriesZone() {
    var created = daily().materialize(LocalDate.of(2026, 1, 5));

    // 6am in Lima (UTC-5) is 11am UTC.
    assertThat(created.getFirst().getStart().toString()).isEqualTo("2026-01-05T11:00Z[UTC]");
  }

  @Test
  void materializeAgainOnlyAddsTheOccurrencesAfterWhatExists() {
    var series = daily();
    var first = series.materialize(LocalDate.of(2026, 1, 7));

    var next = series.materialize(LocalDate.of(2026, 1, 10));

    assertThat(first).hasSize(3);
    assertThat(next).hasSize(3);
    assertThat(next.getFirst().getStart().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 8));
    assertThat(series.getMaterializedUntil()).isEqualTo(LocalDate.of(2026, 1, 10));
    assertThat(series.isFullyMaterialized()).isFalse();
  }

  @Test
  void materializeUpToAnHorizonAlreadyReachedCreatesNothing() {
    var series = daily();
    series.materialize(LocalDate.of(2026, 1, 10));

    assertThat(series.materialize(LocalDate.of(2026, 1, 10))).isEmpty();
  }

  @Test
  void aFullyMaterializedSeriesCreatesNothingMore() {
    var series = eventSeries(church);
    series.materialize(LocalDate.of(2026, 12, 31));

    assertThat(series.materialize(LocalDate.of(2027, 12, 31))).isEmpty();
  }

  @Test
  void theHorizonIsAYearAheadOfTodayOrOfTheFirstDayIfItIsLater() {
    var series = daily();

    assertThat(series.horizonFrom(LocalDate.of(2025, 6, 1))).isEqualTo(LocalDate.of(2027, 1, 5));
    assertThat(series.horizonFrom(LocalDate.of(2026, 3, 1))).isEqualTo(LocalDate.of(2027, 3, 1));
  }

  @Test
  void endOnStopsTheSeriesAfterThatDayAndFixesWhatIsMaterialized() {
    var series = daily();
    series.materialize(LocalDate.of(2026, 12, 31));

    series.endOn(LocalDate.of(2026, 2, 1));

    assertThat(series.getRecurrence().getUntil()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(series.getMaterializedUntil()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(series.isFullyMaterialized()).isTrue();
    assertThat(series.getDeletedTime()).isNull();
  }

  @Test
  void endOnBeforeTheFirstDayDeletesTheSeries() {
    var series = daily();

    series.endOn(LocalDate.of(2026, 1, 4));

    assertThat(series.getDeletedTime()).isNotNull();
  }
}
