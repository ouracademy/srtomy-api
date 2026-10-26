package org.ouracademy.ovejafiel.shared.domain;

/**
 * A request that is well-formed but breaks a domain rule (e.g. a group under a church). Rendered as
 * 400 by {@code GlobalExceptionHandler}; the message is shown to the end user.
 */
public class DomainValidationException extends RuntimeException {

  public DomainValidationException(String message) {
    super(message);
  }
}
