package org.ouracademy.ovejafiel.attendance.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;

@Entity
@Getter
// Hides deleted rows from every query. Rows are never removed: see delete().
@SQLRestriction("deleted_time is null")
public class EventAttendance {

  @Id EventId id;

  @MapsId
  @OneToOne(optional = false)
  @JoinColumn(name = "id")
  Event event;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "eventAttendance")
  List<PersonEventAttendance> personEventAttendances = new ArrayList<>();

  @Column(nullable = false)
  LocalDateTime dateTime;

  /** Visitors who gave no name (or whose name nobody wrote down): only counted. */
  int guestCount;

  LocalDateTime deletedTime;

  public EventAttendance() {
    // jpa only
  }

  public EventAttendance(Event event) {
    this.event = event;
    this.dateTime = LocalDateTime.now();
  }

  public List<PartyId> getPersonIds() {
    return personEventAttendances.stream().map(x -> x.getPerson().getId()).toList();
  }

  /**
   * Soft delete. Not {@code em.remove()}: the cascade on personEventAttendances would hard-delete
   * the person rows. They stay in place, reachable only through this hidden entity.
   */
  public void delete() {
    this.deletedTime = LocalDateTime.now();
  }

  /**
   * Makes {@code people} the attendees. Those who already were keep how and by whom they were
   * marked; the new ones are marked by hand.
   */
  public void setPeople(List<Person> people) {
    var wanted = people.stream().map(Person::getId).collect(Collectors.toSet());
    personEventAttendances.removeIf(mark -> !wanted.contains(mark.getPerson().getId()));
    var present =
        personEventAttendances.stream()
            .map(mark -> mark.getPerson().getId())
            .collect(Collectors.toSet());
    people.stream()
        .filter(person -> present.add(person.getId()))
        .forEach(
            person ->
                personEventAttendances.add(new PersonEventAttendance(this, person, this.dateTime)));
  }

  /** Marks {@code person} as present; false (and nothing changes) when they already were. */
  public boolean markPresent(Person person, CheckInSource source, AppUser markedBy) {
    var already =
        personEventAttendances.stream()
            .anyMatch(mark -> mark.getPerson().getId().equals(person.getId()));
    if (already) {
      return false;
    }
    personEventAttendances.add(
        new PersonEventAttendance(
            this, person, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS), source, markedBy));
    return true;
  }

  public void setGuestCount(int guestCount) {
    if (guestCount < 0) {
      throw new DomainValidationException("La cantidad de visitantes no puede ser negativa");
    }
    this.guestCount = guestCount;
  }
}
