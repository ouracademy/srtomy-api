package org.ouracademy.srtomy.outbox.domain;

public enum OutboxStatus {
  /** Waiting for its first or next attempt. */
  PENDING,
  SENT,
  /** Out of attempts: it will not be retried on its own. */
  FAILED
}
