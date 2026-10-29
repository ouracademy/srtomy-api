package org.ouracademy.ovejafiel.accountability.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

@Entity
@Getter
// A closed accountability (see EndAccountabilitiesOfChildCommand) is history: queries do not see
// it.
@SQLRestriction("deleted_time is null")
public class Accountability extends DomainObject {

  @Id AccountabilityId id = AccountabilityId.generate();

  @ManyToOne(optional = false)
  Party child;

  @ManyToOne(optional = false)
  Party parent;

  @ManyToOne(optional = false)
  @JoinColumn(name = "type")
  AccountabilityType type;

  LocalDateTime deletedTime;

  Accountability() {
    // jpa only
  }

  public Accountability(Party child, Party parent, AccountabilityType type) {
    this.child = child;
    this.parent = parent;
    this.type = type;
  }
}
