package org.ouracademy.ovejafiel.billing.domain;

import java.util.Optional;

/**
 * What a church pays for. Each plan has its own price and maximums (see {@link BillingPolicy}); a
 * church that reaches one moves to the next. The order is the order of the prices.
 */
public enum Plan {
  FREE("Gratis"),
  BASIC("Básico"),
  CHURCH("Iglesia"),
  NETWORK("Red");

  private final String label;

  Plan(String label) {
    this.label = label;
  }

  /** How the plan is called in the messages the end user reads. */
  public String label() {
    return label;
  }

  public boolean isPaid() {
    return this != FREE;
  }

  /** The plan a church moves to when this one is not enough; none after the last. */
  public Optional<Plan> next() {
    var plans = values();
    return ordinal() + 1 < plans.length ? Optional.of(plans[ordinal() + 1]) : Optional.empty();
  }
}
