package org.ouracademy.ovejafiel.whatsapp.domain;

import java.util.List;

/**
 * The templates the app sends, as they are submitted to Meta (all {@code UTILITY}, in Spanish). A
 * template is fixed text with numbered variables, so what each variable holds is part of the
 * contract here. Meta reviews each one for the WhatsApp account of each church.
 */
public enum WhatsAppTemplate {
  /** {1} name of the person, {2} event, {3} date and time, {4} place. */
  EVENT_REMINDER(
      "event_reminder",
      "Hola {{1}}, te recordamos tu inscripción a «{{2}}»: {{3}}. Lugar: {{4}}. ¡Te esperamos!",
      List.of("Ana", "Culto de jóvenes", "sábado 7 de noviembre, 19:30", "Av. Perú 123"),
      true),
  /**
   * {1} name of the person, {2} name of the church. Not sent yet, so not created: it waits for an
   * explicit opt-in (see spec 014).
   */
  WELCOME(
      "welcome",
      "Hola {{1}}, ¡te damos la bienvenida a {{2}}! Si no quieres recibir avisos, responde BAJA.",
      List.of("Ana", "Iglesia Central"),
      false);

  public static final String LANGUAGE = "es";
  public static final String CATEGORY = "UTILITY";

  private final String metaName;
  private final String body;
  private final List<String> examples;
  private final boolean provisioned;

  WhatsAppTemplate(String metaName, String body, List<String> examples, boolean provisioned) {
    this.metaName = metaName;
    this.body = body;
    this.examples = examples;
    this.provisioned = provisioned;
  }

  public String metaName() {
    return metaName;
  }

  public int variables() {
    return examples.size();
  }

  /** The text submitted to Meta, with {@code {{1}}}-style variables. */
  public String body() {
    return body;
  }

  /** One example value per variable: Meta will not review a template without them. */
  public List<String> examples() {
    return examples;
  }

  /** Whether it is created for a church when asked; the ones not sent yet are not. */
  public boolean isProvisioned() {
    return provisioned;
  }
}
