package org.ouracademy.ovejafiel.event.domain;

/** Who, besides the people the event is shared with, can open an event. */
public enum GeneralAccess {
  /** Only whoever has access through the church tree or was shared the event directly. */
  RESTRICTED,
  /** Any user with access somewhere in the church. */
  CHURCH,
  /** Anyone who has the link, signed in or not. */
  ANYONE_WITH_LINK
}
