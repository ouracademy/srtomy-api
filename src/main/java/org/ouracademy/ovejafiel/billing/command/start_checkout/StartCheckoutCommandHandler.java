package org.ouracademy.ovejafiel.billing.command.start_checkout;

/** Prices the period for the people the church has today and asks the gateway for a checkout. */
public interface StartCheckoutCommandHandler {
  StartCheckoutResult handle(StartCheckoutCommand command);
}
