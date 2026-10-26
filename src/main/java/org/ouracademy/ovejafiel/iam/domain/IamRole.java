package org.ouracademy.ovejafiel.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/**
 * A named set of permissions. Predefined roles have no church; custom roles (phase 004) belong to
 * one.
 */
@Entity
@Getter
@SQLRestriction("deleted_time is null")
public class IamRole extends DomainObject {

  @Id IamRoleId id = IamRoleId.generate();

  String name;

  @Column(columnDefinition = "text")
  String description;

  @ManyToOne Organization church;

  List<String> permissions;

  LocalDateTime deletedTime;

  protected IamRole() {
    // jpa only
  }

  private IamRole(Organization church, String name, String description, List<String> permissions) {
    this.church = church;
    this.name = name;
    this.description = description;
    this.permissions = permissions;
  }

  public static IamRole custom(
      Organization church, String name, String description, List<String> permissions) {
    return new IamRole(church, name, description, permissions);
  }

  public boolean isPredefined() {
    return church == null;
  }

  public void update(String name, String description, List<String> permissions) {
    this.name = name;
    this.description = description;
    this.permissions = permissions;
  }

  public void delete() {
    this.deletedTime = LocalDateTime.now();
  }
}
