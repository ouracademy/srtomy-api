package org.ouracademy.ovejafiel.whatsapp.command.confirm_connection;

/** The provider says the customer finished connecting the number. */
public record ConfirmConnectionCommand(
    String idempotencyKey, String customerId, String phoneNumberId) {}
