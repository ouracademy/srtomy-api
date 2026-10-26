package org.ouracademy.ovejafiel.user.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/**
 * Says which person record of a church is the signed-in {@code user}. The positions of that person
 * are what give the user access. A user has at most one person per church and a person at most one
 * user.
 */
@Entity
@Table(name = "user_person")
@Getter
public class UserPersonLink extends DomainObject {

  @Id UserPersonLinkId id = UserPersonLinkId.generate();

  @ManyToOne(optional = false)
  AppUser user;

  @ManyToOne(optional = false)
  Party person;

  @ManyToOne(optional = false)
  Organization church;

  protected UserPersonLink() {
    // jpa only
  }

  public UserPersonLink(AppUser user, Party person, Organization church) {
    this.user = user;
    this.person = person;
    this.church = church;
  }
}
