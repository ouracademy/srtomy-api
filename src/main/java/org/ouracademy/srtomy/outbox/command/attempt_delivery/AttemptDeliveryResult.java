package org.ouracademy.srtomy.outbox.command.attempt_delivery;

/** What the queue should do next: only {@code RETRY} asks it to call again later. */
public record AttemptDeliveryResult(Outcome outcome) {

  public enum Outcome {
    SENT,
    /** Delivered by an earlier call, or given up: nothing to do. */
    ALREADY_DONE,
    /** The transaction that enqueued it did not commit. */
    NOT_FOUND,
    /** Gave up: the failure is permanent or the attempts ran out. */
    FAILED,
    RETRY
  }

  public boolean shouldRetry() {
    return outcome == Outcome.RETRY;
  }
}
