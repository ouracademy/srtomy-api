package org.ouracademy.srtomy.accountability.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.shared.domain.DomainObject;

@Entity
@Getter
@SQLDelete(sql = "UPDATE accountability_type SET deleted_time = now() WHERE id=?")
@SQLRestriction("deleted_time is null")
public class AccountabilityType extends DomainObject {

  @Id AccountabilityTypeId id = AccountabilityTypeId.generate();

  String name;

  @ManyToOne Party party;

  LocalDateTime deletedTime;

  @Enumerated(EnumType.STRING)
  Category category;

  public enum Category {
    /** A ministerial position (cargo) owned by a church; grants no access. */
    POSITION,
    MEMBER_PROGRESS_STATE
  }

  AccountabilityType() {
    // jpa only
  }

  public AccountabilityType(String name, Party party, Category category) {
    this.name = name;
    this.party = party;
    this.category = category;
  }

  public boolean isPosition() {
    return category == Category.POSITION;
  }

  public void rename(String name) {
    this.name = name;
  }
}
