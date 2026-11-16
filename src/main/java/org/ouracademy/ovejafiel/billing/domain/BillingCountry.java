package org.ouracademy.ovejafiel.billing.domain;

/** Where a church pays from: it decides the currency and the price list. */
public enum BillingCountry {
  PE("PEN"),
  CO("COP");

  private final String currency;

  BillingCountry(String currency) {
    this.currency = currency;
  }

  public String currency() {
    return currency;
  }
}
