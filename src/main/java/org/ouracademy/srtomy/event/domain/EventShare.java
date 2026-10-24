package org.ouracademy.srtomy.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.srtomy.shared.domain.DomainObject;
import org.ouracademy.srtomy.user.domain.AppUser;

/**
 * An event shared with the person whose verified email is {@code email}. It applies to them even
 * before they have an account.
 */
@Entity
@Getter
public class EventShare extends DomainObject {

  @Id EventShareId id = EventShareId.generate();

  @ManyToOne(optional = false)
  Event event;

  @Column(nullable = false)
  String email;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  ShareRole role;

  @ManyToOne
  @JoinColumn(name = "shared_by")
  AppUser sharedBy;

  protected EventShare() {
    // jpa only
  }

  public EventShare(Event event, String email, ShareRole role, AppUser sharedBy) {
    this.event = event;
    this.email = email;
    this.role = role;
    this.sharedBy = sharedBy;
  }

  public boolean isFor(String otherEmail) {
    return otherEmail != null && email.equalsIgnoreCase(otherEmail.trim());
  }
}
