package org.ouracademy.srtomy.attendance.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.shared.utils.JpaLists;

@Entity
@Getter
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

  public void setPeople(List<Person> people) {
    JpaLists.set(
        this.personEventAttendances,
        people.stream()
            .map(person -> new PersonEventAttendance(this, person, this.dateTime))
            .toList());
  }
}
