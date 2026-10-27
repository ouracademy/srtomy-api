package org.ouracademy.ovejafiel.accountability.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

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
   * For a position: the iam roles whose permissions whoever holds it gets. Kept as ids, since
   * accountability does not depend on the iam module's entities.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "position_role", joinColumns = @JoinColumn(name = "position_id"))
  @Column(name = "role_id")
  Set<IamRoleId> roleIds = new HashSet<>();

  /** For a position: loose permissions on top of the roles. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "position_permission", joinColumns = @JoinColumn(name = "position_id"))
  @Column(name = "permission")
  Set<String> permissions = new HashSet<>();

  public enum Category {
    /** A ministerial position (cargo) owned by a church; may give permissions. */
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

  public AccountabilityType(
      String name,
      Party party,
      Category category,
      Set<IamRoleId> roleIds,
      Set<String> permissions) {
    this(name, party, category);
    setAccess(roleIds, permissions);
  }

  /** Whether holding the position gives anything in the app; otherwise it is only a label. */
  public boolean hasAccess() {
    return !roleIds.isEmpty() || !permissions.isEmpty();
  }

  public void setAccess(Set<IamRoleId> roleIds, Set<String> permissions) {
    this.roleIds.clear();
    this.roleIds.addAll(roleIds);
    this.permissions.clear();
    this.permissions.addAll(permissions);
  }

  public boolean isPosition() {
    return category == Category.POSITION;
  }

  public void rename(String name) {
    this.name = name;
  }
}
