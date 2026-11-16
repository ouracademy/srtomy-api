package org.ouracademy.ovejafiel.billing.command.sync_payment;

/** Safe to repeat: a payment already taken into account changes nothing. */
public interface SyncPaymentCommandHandler {
  SyncPaymentResult handle(SyncPaymentCommand command);
}
