package org.ouracademy.srtomy.invitation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.shared.domain.DomainObject;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.AppUser;

/**
 * An invitation to use the app: whoever signs in with the verified {@code email} before it expires
 * is linked to a person of the church and takes {@code position} in {@code organization}.
 */
@Entity
@Getter
public class Invitation extends DomainObject {

  @Id InvitationId id = InvitationId.generate();

  @Column(nullable = false)
  String email;

  @ManyToOne(optional = false)
  Organization organization;

  /** Null when the invitation only links the account. */
  @ManyToOne AccountabilityType position;

  /** Null when the person is created on acceptance, from {@code firstName} and {@code lastName}. */
  @ManyToOne Person person;

  String firstName;
  String lastName;

  @Column(nullable = false, unique = true)
  String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  InvitationStatus status = InvitationStatus.PENDING;

  @Column(nullable = false)
  LocalDateTime expiresAt;

  @ManyToOne(optional = false)
  @JoinColumn(name = "invited_by")
  AppUser invitedBy;

  @ManyToOne
  @JoinColumn(name = "accepted_by")
  AppUser acceptedBy;

  protected Invitation() {
    // jpa only
  }

  public Invitation(
      String email,
      Organization organization,
      AccountabilityType position,
      Person person,
      String firstName,
      String lastName,
      String tokenHash,
      LocalDateTime expiresAt,
      AppUser invitedBy) {
    this.email = email;
    this.organization = organization;
    this.position = position;
    this.person = person;
    this.firstName = firstName;
    this.lastName = lastName;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.invitedBy = invitedBy;
  }

  public InvitationStatus effectiveStatus(LocalDateTime now) {
    return status == InvitationStatus.PENDING && !expiresAt.isAfter(now)
        ? InvitationStatus.EXPIRED
        : status;
  }

  /** Fails unless the invitation can still be accepted. */
  public void requireAcceptable(LocalDateTime now) {
    switch (effectiveStatus(now)) {
      case PENDING -> {}
      case EXPIRED -> throw new DomainValidationException("La invitación venció");
      case ACCEPTED -> throw new DomainValidationException("La invitación ya fue aceptada");
      case REVOKED -> throw new DomainValidationException("La invitación fue cancelada");
    }
  }

  public boolean isFor(String otherEmail) {
    return otherEmail != null && email.equalsIgnoreCase(otherEmail.trim());
  }

  public void accept(AppUser user) {
    status = InvitationStatus.ACCEPTED;
    acceptedBy = user;
  }

  public void revoke() {
    if (status == InvitationStatus.PENDING) {
      status = InvitationStatus.REVOKED;
    }
  }
}
