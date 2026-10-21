package org.ouracademy.srtomy.party.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.srtomy.shared.domain.DomainObject;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Party extends DomainObject {

  @Id PartyId id = PartyId.generate();

  @Setter protected String name;

  @Setter
  @Column(columnDefinition = "text")
  protected String description;

  // TODO: validate
  @Setter protected String phoneNumber;

  @Setter
  @Column(columnDefinition = "text")
  protected String address;
}
