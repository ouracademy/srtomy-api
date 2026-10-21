package org.ouracademy.srtomy.organization.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import org.ouracademy.srtomy.party.domain.Party;

@Entity
@Getter
public class Organization extends Party {

  @Enumerated(EnumType.STRING)
  Type type;

  public enum Type {
    CHURCH,
    LOCATION,
    GROUP,
    MINISTRY
  }

  protected Organization() {
    // jpa only
  }

  public Organization(String name, Type type) {
    this.name = name;
    this.type = type;
  }

  /** Only groups and ministries can be deleted; churches and locations cannot. */
  public boolean isDeletable() {
    return type == Type.GROUP || type == Type.MINISTRY;
  }
}
