package org.ouracademy.ovejafiel.billing.command.sync_payment;

/** The gateway says something changed in the payment {@code gatewayPaymentId}. */
public record SyncPaymentCommand(String gatewayPaymentId) {}
