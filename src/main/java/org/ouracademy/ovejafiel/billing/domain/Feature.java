package org.ouracademy.ovejafiel.billing.domain;

/**
 * What only some plans include. The AI assistant, WhatsApp through the number of Sr. Tommy and the
 * groups, events and attendance are in every plan and have no entry here (their quotas are in
 * {@link BillingPolicy.Limits}).
 */
public enum Feature {
  MASS_EMAIL("Correos a grupos de personas", Plan.CHURCH),
  FINANCES("Ofrendas y estados de cuenta", Plan.CHURCH),
  VOLUNTEERS("Turnos y voluntarios", Plan.CHURCH),
  EXCEL_EXPORT("Exportar a Excel", Plan.BASIC),
  CUSTOM_ROLES("Roles personalizados", Plan.BASIC),
  /** Connecting a WhatsApp number of the church itself; the plans below NETWORK buy it apart. */
  OWN_NUMBER("Número de WhatsApp propio de la iglesia", Plan.NETWORK);

  private final String label;
  private final Plan cheapestPlan;

  Feature(String label, Plan cheapestPlan) {
    this.label = label;
    this.cheapestPlan = cheapestPlan;
  }

  /** How the feature is called in the messages the end user reads. */
  public String label() {
    return label;
  }

  /** The cheapest plan that includes the feature, to tell who asks where to go. */
  public Plan cheapestPlan() {
    return cheapestPlan;
  }
}
