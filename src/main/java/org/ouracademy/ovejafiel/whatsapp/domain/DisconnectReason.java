package org.ouracademy.ovejafiel.whatsapp.domain;

/** Why a church's number stopped working. */
public enum DisconnectReason {
  /** The pastor disconnected it from the portal. */
  PASTOR,
  /** The number was removed from our project at the provider. */
  NUMBER_DELETED,
  /** Meta offboarded the number from the Cloud API. */
  NUMBER_OFFBOARDED,
  /** The pastor (or Meta) removed our access, or uninstalled the app. */
  ACCESS_REMOVED,
  /** Meta disabled the WhatsApp Business Account. */
  ACCOUNT_DISABLED;

  /** Only the pastor can bring back a number the pastor disconnected; the rest may heal. */
  public boolean healsOnItsOwn() {
    return this != PASTOR;
  }
}
