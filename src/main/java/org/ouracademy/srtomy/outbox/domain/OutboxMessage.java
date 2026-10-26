package org.ouracademy.srtomy.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.srtomy.shared.domain.DomainObject;

/**
 * Something to do against a system outside our database (today: send an email), saved in the same
 * transaction as the change that asked for it, and done afterwards with retries. See "Operaciones
 * con sistemas externos" in docs/architecture.md.
 */
@Entity
@Getter
public class OutboxMessage extends DomainObject {

  /**
   * After the n-th failed attempt, wait this long; the last failure gives up. The scheduler drains
   * every 10 minutes, so a shorter wait would not retry any sooner.
   */
  private static final Duration[] BACKOFF = {
    Duration.ofMinutes(10),
    Duration.ofMinutes(30),
    Duration.ofHours(2),
    Duration.ofHours(6),
    Duration.ofHours(12)
  };

  public static final int MAX_ATTEMPTS = BACKOFF.length + 1;

  private static final int MAX_ERROR_LENGTH = 1000;

  @Id OutboxMessageId id = OutboxMessageId.generate();

  /** Picks the handler that does the work, for example {@code EMAIL}. */
  @Column(nullable = false)
  String type;

  @Column(nullable = false)
  String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  OutboxStatus status = OutboxStatus.PENDING;

  @Column(nullable = false)
  int attempts;

  @Column(nullable = false)
  LocalDateTime nextAttemptAt;

  String lastError;

  LocalDateTime sentAt;

  protected OutboxMessage() {
    // jpa only
  }

  public OutboxMessage(String type, String payload, LocalDateTime now) {
    this.type = type;
    this.payload = payload;
    this.nextAttemptAt = now;
  }

  public void markSent(LocalDateTime now) {
    status = OutboxStatus.SENT;
    attempts++;
    sentAt = now;
    lastError = null;
  }

  /** Schedules the next attempt, or gives up when there are no more. Returns the new status. */
  public OutboxStatus markFailed(String error, LocalDateTime now) {
    attempts++;
    lastError =
        error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
    if (attempts >= MAX_ATTEMPTS) {
      status = OutboxStatus.FAILED;
    } else {
      nextAttemptAt = now.plus(BACKOFF[attempts - 1]);
    }
    return status;
  }

  /** Nothing will be retried, whatever the attempts left (for example an unknown type). */
  public void giveUp(String error) {
    status = OutboxStatus.FAILED;
    lastError = error;
  }
}
