package org.ouracademy.srtomy.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.srtomy.domain.DomainObject;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Party extends DomainObject {

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
