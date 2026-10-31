package org.ouracademy.ovejafiel.whatsapp.domain;

/**
 * The templates the app sends, as they are approved by Meta (all {@code UTILITY}, in Spanish). A
 * template is fixed text with numbered variables, so what each variable holds is part of the
 * contract here.
 */
public enum WhatsAppTemplate {
  /** {1} name of the person, {2} event, {3} date and time, {4} place. */
  EVENT_REMINDER("event_reminder", 4),
  /** {1} name of the person, {2} name of the church. */
  WELCOME("welcome", 2);

  public static final String LANGUAGE = "es";

  private final String metaName;
  private final int variables;

  WhatsAppTemplate(String metaName, int variables) {
    this.metaName = metaName;
    this.variables = variables;
  }

  public String metaName() {
    return metaName;
  }

  public int variables() {
    return variables;
  }
}
