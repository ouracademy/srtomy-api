package org.ouracademy.ovejafiel.invitation.domain;

/**
 * What the database stores. A pending invitation past its deadline is shown as {@link #EXPIRED}
 * (see {@link Invitation#effectiveStatus}) without a job rewriting rows.
 */
public enum InvitationStatus {
  PENDING,
  ACCEPTED,
  REVOKED,
  EXPIRED
}
