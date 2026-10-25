package org.ouracademy.srtomy.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.ouracademy.srtomy.shared.domain.DomainObject;
import org.ouracademy.srtomy.shared.domain.RandomSlugs;
import org.ouracademy.srtomy.user.domain.UserId;

/**
 * The private calendar feed of a user, to subscribe to from Google Calendar, Apple Calendar...
 * Those apps cannot send a token header, so the unguessable {@code token} in the address is the
 * credential; a new one revokes the old address.
 */
@Entity
@Getter
public class CalendarFeed extends DomainObject {

  @Id
  @Column(name = "user_id")
  UserId userId;

  String token = RandomSlugs.next();

  protected CalendarFeed() {
    // jpa only
  }

  public CalendarFeed(UserId userId) {
    this.userId = userId;
  }

  public void renew() {
    this.token = RandomSlugs.next();
  }
}
