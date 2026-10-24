package org.ouracademy.srtomy.accountability.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
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

  /**
   * For a position: the access level (an iam role) that whoever holds it gets, or null when it is
   * only a label. Kept as an id, since accountability does not depend on the iam module's entities.
   */
  @Column(name = "iam_role_id")
  IamRoleId accessRoleId;

  public enum Category {
    /** A ministerial position (cargo) owned by a church; may carry an access level. */
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

  public AccountabilityType(String name, Party party, Category category, IamRoleId accessRoleId) {
    this(name, party, category);
    this.accessRoleId = accessRoleId;
  }

  public boolean hasAccess() {
    return accessRoleId != null;
  }

  public void setAccessRoleId(IamRoleId accessRoleId) {
    this.accessRoleId = accessRoleId;
  }

  public boolean isPosition() {
    return category == Category.POSITION;
  }

  public void rename(String name) {
    this.name = name;
  }
}
