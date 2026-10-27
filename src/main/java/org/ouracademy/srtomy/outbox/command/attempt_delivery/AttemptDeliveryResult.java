package org.ouracademy.srtomy.outbox.command.attempt_delivery;

/**
 * What the queue should do next: {@code RETRY} and {@code NOT_FOUND} ask it to call again later.
 */
public record AttemptDeliveryResult(Outcome outcome) {

  public enum Outcome {
    SENT,
    /** Delivered by an earlier call, or given up: nothing to do. */
    ALREADY_DONE,
    /**
     * Not there (yet): the transaction that enqueued it may not have committed when the task
     * arrived. The queue calls again; if it never commits, the attempts run out harmlessly.
     */
    NOT_FOUND,
    /** Gave up: the failure is permanent or the attempts ran out. */
    FAILED,
    RETRY
  }

  public boolean shouldRetry() {
    return outcome == Outcome.RETRY || outcome == Outcome.NOT_FOUND;
  }
}
