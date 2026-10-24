package org.ouracademy.srtomy.iam.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.shared.domain.DomainObject;
import org.ouracademy.srtomy.user.domain.AppUser;

/** Grants {@code role} to {@code user} on {@code organization} and every node below it. */
@Entity
@Getter
public class RoleBinding extends DomainObject {

  @Id RoleBindingId id = RoleBindingId.generate();

  @ManyToOne(optional = false)
  AppUser user;

  @ManyToOne(optional = false)
  IamRole role;

  @ManyToOne(optional = false)
  Organization organization;

  @ManyToOne
  @JoinColumn(name = "granted_by")
  AppUser grantedBy;

  protected RoleBinding() {
    // jpa only
  }

  public RoleBinding(AppUser user, IamRole role, Organization organization, AppUser grantedBy) {
    this.user = user;
    this.role = role;
    this.organization = organization;
    this.grantedBy = grantedBy;
  }
}
