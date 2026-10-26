package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.ZonedDateTime;
import lombok.Getter;

@Entity
@DiscriminatorValue("1")
@Getter
public class SingleEvent extends Event {
  @ManyToOne RecurrentEvent recurrentEvent;

  public SingleEvent() {
    // jpa only
  }

  public SingleEvent(String title, ZonedDateTime start, ZonedDateTime end) {
    this.title = title;
    this.start = start;
    this.end = end;
  }
}
