package org.ouracademy.ovejafiel.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/**
 * Someone who signs in (identified by the identity provider's subject). Not the same as a church's
 * person record: most people never sign in, and a user may belong to several churches.
 */
@Entity
@Getter
public class AppUser extends DomainObject {

  /** Subject of the users made for people who write on WhatsApp and have no account. */
  public static final String WHATSAPP_SUBJECT_PREFIX = "whatsapp:";

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

  /** A user for someone known only by their phone: it has the Member level and nothing more. */
  public static AppUser forWhatsApp(PartyId personId) {
    return new AppUser(WHATSAPP_SUBJECT_PREFIX + personId.value());
  }

  public boolean isWhatsAppIdentity() {
    return subject.startsWith(WHATSAPP_SUBJECT_PREFIX);
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
