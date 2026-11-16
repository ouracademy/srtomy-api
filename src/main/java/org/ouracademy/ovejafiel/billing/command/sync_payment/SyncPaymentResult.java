package org.ouracademy.ovejafiel.billing.command.sync_payment;

/** {@code known}: whether the payment is one of ours. */
public record SyncPaymentResult(boolean known, boolean paid) {}
