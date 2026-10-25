package org.ouracademy.srtomy.event.domain;

/** How much someone can do with an event, from nothing to everything (including sharing it). */
public enum AccessLevel {
  NONE,
  VIEWER,
  EDITOR,
  MANAGER;

  public boolean atLeast(AccessLevel other) {
    return compareTo(other) >= 0;
  }

  public AccessLevel or(AccessLevel other) {
    return atLeast(other) ? this : other;
  }
}
