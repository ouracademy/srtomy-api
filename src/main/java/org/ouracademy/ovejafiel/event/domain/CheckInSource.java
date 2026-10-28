package org.ouracademy.ovejafiel.event.domain;

/** How someone was marked as present. */
public enum CheckInSource {
  /** A pastor or usher marked them. */
  MANUAL,
  /** They scanned the QR code of the event. */
  QR_EVENT,
  /** An usher scanned the QR code of their ticket. */
  QR_TICKET
}
