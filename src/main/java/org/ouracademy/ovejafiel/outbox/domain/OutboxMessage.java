package org.ouracademy.ovejafiel.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/**
 * Something to deliver through a system outside our database (an email, a WhatsApp message), saved
 * in the same transaction as the change that asked for it and delivered afterwards by a Cloud Tasks
 * task, which retries it. See "Trabajo programado y sistemas externos" in docs/code-style.md.
 */
@Entity
@Getter
public class OutboxMessage extends DomainObject {

  private static final int MAX_ERROR_LENGTH = 1000;

  @Id OutboxMessageId id = OutboxMessageId.generate();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  OutboxChannel channel;

  /** What it is about, for whoever reads the table: INVITATION, EVENT_SHARE... */
  @Column(nullable = false)
  String type;

  @Column(nullable = false)
  String recipient;

  /** Null once the message is done: it may hold a secret (the link of an invitation). */
  String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  OutboxStatus status = OutboxStatus.PENDING;

  @Column(nullable = false)
  int attempts;

  String lastError;

  String providerMessageId;

  LocalDateTime sentAt;

  protected OutboxMessage() {
    // jpa only
  }

  public OutboxMessage(OutboxChannel channel, String type, String recipient, String payload) {
    this.channel = channel;
    this.type = type;
    this.recipient = recipient;
    this.payload = payload;
  }

  public void markSent(String providerMessageId, LocalDateTime now) {
    attempts++;
    status = OutboxStatus.SENT;
    sentAt = now;
    this.providerMessageId = providerMessageId;
    lastError = null;
    payload = null;
  }

  /** An attempt that did not work but may work later: the queue calls again. */
  public void recordFailedAttempt(String error) {
    attempts++;
    lastError = truncated(error);
  }

  /** Nothing will be tried again. */
  public void markFailed(String error) {
    status = OutboxStatus.FAILED;
    lastError = truncated(error);
    payload = null;
  }

  /** Withdraws it if it still waits; returns whether it did. */
  public boolean cancel() {
    if (status != OutboxStatus.PENDING) {
      return false;
    }
    status = OutboxStatus.CANCELLED;
    payload = null;
    return true;
  }

  private static String truncated(String error) {
    return error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
  }
}
