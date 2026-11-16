package org.ouracademy.ovejafiel.billing.domain;

/**
 * What only the paid plan includes. WhatsApp is the exception: the free plan may try it for a few
 * days (see {@link BillingPolicy#whatsappTrialDays()}).
 */
public enum Feature {
  WHATSAPP("WhatsApp de la iglesia"),
  MASS_EMAIL("Correos a grupos de personas"),
  FINANCES("Ofrendas y estados de cuenta"),
  VOLUNTEERS("Turnos y voluntarios"),
  EXCEL_EXPORT("Exportar a Excel");

  private final String label;

  Feature(String label) {
    this.label = label;
  }

  /** How the feature is called in the messages the end user reads. */
  public String label() {
    return label;
  }
}
