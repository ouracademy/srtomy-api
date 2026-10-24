package org.ouracademy.srtomy.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.shared.domain.DomainObject;

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
}
