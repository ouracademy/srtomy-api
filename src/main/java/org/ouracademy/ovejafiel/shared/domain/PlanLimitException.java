package org.ouracademy.ovejafiel.shared.domain;

/**
 * The plan of the church does not allow what was asked: a feature of the paid plan, or one more
 * than the free plan includes. Rendered as 402 by {@code GlobalExceptionHandler}; the message is
 * shown to the end user and {@code code} names the limit so a client can offer the upgrade.
 */
public class PlanLimitException extends RuntimeException {

  private final String code;

  public PlanLimitException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
