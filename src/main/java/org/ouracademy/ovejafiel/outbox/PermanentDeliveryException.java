package org.ouracademy.ovejafiel.outbox;

/** Delivery failed in a way that trying again will not fix: the message is given up at once. */
public class PermanentDeliveryException extends RuntimeException {

  public PermanentDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
