package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Getter;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

/** Something people who follow the event should know, shown on its page. */
@Entity
@Getter
public class EventNotice {

  public static final int MAX_LENGTH = 1000;

  @Id UUID id = UUID.randomUUID();

  @ManyToOne(optional = false)
  Event event;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  NoticeKind kind;

  @Column(nullable = false)
  String message;

  @Column(nullable = false)
  LocalDateTime createdAt;

  protected EventNotice() {
    // jpa only
  }

  public EventNotice(Event event, NoticeKind kind, String message) {
    if (message == null || message.isBlank()) {
      throw new DomainValidationException("Escriba el aviso");
    }
    if (message.trim().length() > MAX_LENGTH) {
      throw new DomainValidationException(
          "El aviso es muy largo: máximo " + MAX_LENGTH + " caracteres");
    }
    this.event = event;
    this.kind = kind;
    this.message = message.trim();
    // The database keeps microseconds: what we answer now must be what it will give back.
    this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
  }
}
