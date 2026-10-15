package org.ouracademy.srtomy.organization;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import org.ouracademy.srtomy.person.Party;

@Entity
@Getter
public class Organization extends Party {

  @Enumerated(EnumType.STRING)
  Type type;

  enum Type {
    CHURCH,
    LOCATION,
    GROUP,
    MINISTRY
  }

  Organization() {
    // jpa only
  }

  Organization(String name) {
    this.name = name;
  }
}
