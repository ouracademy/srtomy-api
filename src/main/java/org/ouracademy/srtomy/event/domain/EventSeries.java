package org.ouracademy.srtomy.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.shared.domain.DomainObject;

/**
 * The rule behind a repeating event. Its occurrences are {@link Event}s (attendance, sign-ups and
 * sharing hang from each one), created ahead up to a rolling horizon: {@link #materialize} adds the
 * missing ones, so a series that never ends keeps getting occurrences.
 */
@Entity
@Getter
@SQLRestriction("deleted_time is null")
public class EventSeries extends DomainObject {

  /** How far ahead occurrences are created. */
  public static final int MATERIALIZED_MONTHS = 12;

  @Id EventSeriesId id = EventSeriesId.generate();

  @Column(name = "name")
  String title;

  @Setter String address;

  @Setter String description;

  @Setter
  @Column(name = "is_a_course")
  Boolean isACourse = false;

  @Setter
  @ManyToOne(optional = false)
  Organization organization;

  @Setter
  @OneToMany
  @JoinTable(name = "event_series_speakers")
  List<Person> speakers = new ArrayList<>();

  /** First occurrence, as wall-clock time in {@link #timeZone}. */
  LocalDateTime startLocal;

  @Column(name = "duration_seconds")
  long durationSeconds;

  /** IANA zone, e.g. America/Lima: the hour of every occurrence is kept across summer time. */
  String timeZone;

  @Embedded Recurrence recurrence;

  /** Local date up to which occurrences exist. */
  LocalDate materializedUntil;

  /** True when the recurrence has no occurrence beyond {@link #materializedUntil}. */
  boolean fullyMaterialized;

  LocalDateTime deletedTime;

  protected EventSeries() {
    // jpa only
  }

  public EventSeries(
      String title,
      LocalDateTime startLocal,
      Duration duration,
      ZoneId timeZone,
      Recurrence recurrence) {
    this.title = title;
    this.startLocal = startLocal;
    this.durationSeconds = duration.toSeconds();
    this.timeZone = timeZone.getId();
    this.recurrence = recurrence;
    this.materializedUntil = startLocal.toLocalDate().minusDays(1);
  }

  public ZoneId zone() {
    return ZoneId.of(timeZone);
  }

  public Duration duration() {
    return Duration.ofSeconds(durationSeconds);
  }

  /** Creates the occurrences missing up to {@code horizon}; the caller saves them. */
  public List<Event> materialize(LocalDate horizon) {
    if (fullyMaterialized || !horizon.isAfter(materializedUntil)) {
      return List.of();
    }
    var expansion = recurrence.expand(startLocal, zone(), horizon);
    var created =
        expansion.starts().stream()
            .filter(start -> start.toLocalDate().isAfter(materializedUntil))
            .map(this::occurrence)
            .toList();
    materializedUntil = horizon;
    fullyMaterialized = expansion.exhausted();
    return created;
  }

  /** Horizon for a series seen today: a year ahead of today, or of its first day if later. */
  public LocalDate horizonFrom(LocalDate today) {
    var from = today.isAfter(startLocal.toLocalDate()) ? today : startLocal.toLocalDate();
    return from.plusMonths(MATERIALIZED_MONTHS);
  }

  private Event occurrence(java.time.ZonedDateTime start) {
    var utcStart = start.withZoneSameInstant(ZoneId.of("UTC"));
    var event = new Event(title, utcStart, utcStart.plus(duration()));
    event.organization = organization;
    event.address = address;
    event.description = description;
    event.isACourse = isACourse;
    event.speakers = new ArrayList<>(speakers);
    event.series = this;
    return event;
  }

  /** The series stops repeating after {@code lastDay}; a series left without days is deleted. */
  public void endOn(LocalDate lastDay) {
    if (lastDay.isBefore(startLocal.toLocalDate())) {
      delete();
      return;
    }
    recurrence = recurrence.endingOn(lastDay);
    if (materializedUntil.isAfter(lastDay)) {
      materializedUntil = lastDay;
    }
    fullyMaterialized = true;
  }

  public void delete() {
    this.deletedTime = LocalDateTime.now();
  }
}
