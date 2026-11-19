package org.ouracademy.ovejafiel.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/**
 * What the AI assistant used for a church in a month. Read only: the counters change with the
 * atomic statements of {@code AiUsageRepository}, never by saving this entity.
 */
@Entity
@Table(name = "billing_ai_usage")
@Getter
public class AiUsage extends DomainObject {

  @Id AiUsageId id;

  @Column(nullable = false)
  PartyId organizationId;

  /** First day of the month. */
  @Column(nullable = false)
  LocalDate month;

  /** Answers the assistant gave (reserved before calling the model). */
  int messages;

  long inputTokens;

  long outputTokens;

  long cacheReadTokens;

  long cacheWriteTokens;

  /** WhatsApp reminders (Meta templates) queued this month. */
  int whatsappReminders;

  @Column(nullable = false)
  BigDecimal costUsd;

  protected AiUsage() {
    // jpa only
  }
}
