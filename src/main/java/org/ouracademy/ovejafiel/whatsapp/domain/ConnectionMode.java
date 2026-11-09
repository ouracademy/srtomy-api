package org.ouracademy.ovejafiel.whatsapp.domain;

/** How the pastor connects the number (names as Kapso writes them, in lower case). */
public enum ConnectionMode {
  /** The WhatsApp Business app keeps working next to the API. */
  COEXISTENCE,
  /** The number is for the API only. */
  DEDICATED;

  public String kapsoValue() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
