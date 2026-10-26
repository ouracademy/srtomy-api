package org.ouracademy.srtomy.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records the emails the outbox hands to the provider, for integration tests of other modules (they
 * may not touch {@code notification.gateway}). Import {@link TestEmailConfig} to use it.
 */
public class SentEmails {

  public record Email(String idempotencyKey, String to, String subject, String text) {}

  private final List<Email> sent = new CopyOnWriteArrayList<>();
  private volatile RuntimeException failure;

  public void reset() {
    sent.clear();
    failure = null;
  }

  /** Every attempt fails with this until {@link #reset()}, like a provider that rejects them. */
  public void failWith(RuntimeException failure) {
    this.failure = failure;
  }

  void deliver(Email email) {
    if (failure != null) {
      throw failure;
    }
    sent.add(email);
  }

  public List<Email> all() {
    return List.copyOf(sent);
  }

  public Email last() {
    return sent.getLast();
  }
}
