package org.ouracademy.ovejafiel.shared.domain;

/**
 * A request that cannot be done in the current state of things, but could be once the caller
 * confirms or changes something. Rendered as 409 by {@code GlobalExceptionHandler}; the message is
 * shown to the end user and {@code code} lets a client react to the case.
 */
public class ConflictException extends RuntimeException {

  private final String code;

  public ConflictException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
