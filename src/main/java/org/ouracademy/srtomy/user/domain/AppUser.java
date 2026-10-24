package org.ouracademy.srtomy.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.ouracademy.srtomy.shared.domain.DomainObject;

/**
 * Someone who signs in (identified by the identity provider's subject). Not the same as a church's
 * person record: most people never sign in, and a user may belong to several churches.
 */
@Entity
@Getter
public class AppUser extends DomainObject {

  @Id UserId id = UserId.generate();

  @Column(name = "auth0_sub", nullable = false, unique = true)
  String subject;

  String email;

  boolean emailVerified;

  protected AppUser() {
    // jpa only
  }

  public AppUser(String subject) {
    this.subject = subject;
  }

  /**
   * Refreshes the contact data from the latest token. A token without an email keeps the stored
   * one, since not every token carries the claim.
   */
  public void updateContact(String email, boolean emailVerified) {
    if (email == null || email.isBlank()) {
      return;
    }
    this.email = email;
    this.emailVerified = emailVerified;
  }
}
