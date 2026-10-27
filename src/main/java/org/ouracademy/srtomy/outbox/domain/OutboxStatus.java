package org.ouracademy.srtomy.outbox.domain;

public enum OutboxStatus {
  /** Waiting for its first or next attempt. */
  PENDING,
  SENT,
  /** Out of attempts: it will not be retried on its own. */
  FAILED,
  /** Withdrawn while it waited: the reason for sending it is gone. */
  CANCELLED
}
