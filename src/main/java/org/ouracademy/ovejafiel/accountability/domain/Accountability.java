package org.ouracademy.ovejafiel.accountability.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

@Entity
@Getter
// import org.hibernate.annotations.SQLDelete;
// import org.hibernate.annotations.SQLRestriction;
// @SQLDelete(sql = "UPDATE accountability SET deleted_time = now() WHERE id=?")
// @SQLRestriction("deleted_time is null")
public class Accountability extends DomainObject {

  @Id AccountabilityId id = AccountabilityId.generate();

  @ManyToOne(optional = false)
  Party child;

  @ManyToOne(optional = false)
  Party parent;

  @ManyToOne(optional = false)
  @JoinColumn(name = "type")
  AccountabilityType type;

  LocalDateTime deletedTime; // TODO: remove soft delete

  Accountability() {
    // jpa only
  }

  public Accountability(Party child, Party parent, AccountabilityType type) {
    this.child = child;
    this.parent = parent;
    this.type = type;
  }
}
