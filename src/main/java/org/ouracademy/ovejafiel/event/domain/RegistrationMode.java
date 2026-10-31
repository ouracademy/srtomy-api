package org.ouracademy.ovejafiel.event.domain;

/** How a child of a composite event is entered. Ignored for events without a parent. */
public enum RegistrationMode {
  /** The ticket of the parent's registration is valid. */
  INHERIT,
  /** The child has its own registration setup and tickets. */
  OWN,
  /** No registration needed. */
  OPEN
}
