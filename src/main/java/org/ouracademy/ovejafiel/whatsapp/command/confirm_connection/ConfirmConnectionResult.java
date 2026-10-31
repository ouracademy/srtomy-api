package org.ouracademy.ovejafiel.whatsapp.command.confirm_connection;

public enum ConfirmConnectionResult {
  CONNECTED,
  /** The same delivery arrived again. */
  ALREADY_HANDLED,
  /** The customer is not one of ours. */
  UNKNOWN_CUSTOMER
}
