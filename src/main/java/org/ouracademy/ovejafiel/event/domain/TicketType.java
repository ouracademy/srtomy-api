package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.ZonedDateTime;
import lombok.Getter;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

/**
 * A kind of ticket of an event: what it is called, what it costs, how many there are, until when.
 */
@Entity
@Getter
public class TicketType extends DomainObject {

  @Id TicketTypeId id = TicketTypeId.generate();

  @ManyToOne(optional = false)
  Event event;

  String name;

  @Column(name = "price_cents")
  int priceCents;

  String currency;

  /** Null is unlimited. */
  Integer quantity;

  ZonedDateTime salesStart;

  ZonedDateTime salesEnd;

  int position;

  protected TicketType() {
    // jpa only
  }

  public TicketType(Event event, int position) {
    this.event = event;
    this.position = position;
  }

  /** Replaces what describes the ticket, checking it makes sense. */
  public void describe(
      String name,
      int priceCents,
      String currency,
      Integer quantity,
      ZonedDateTime salesStart,
      ZonedDateTime salesEnd) {
    if (name == null || name.isBlank()) {
      throw new DomainValidationException("Indique el nombre del ticket");
    }
    if (priceCents < 0) {
      throw new DomainValidationException("El precio no puede ser negativo");
    }
    if (quantity != null && quantity < 1) {
      throw new DomainValidationException("La cantidad de tickets debe ser mayor que cero");
    }
    if (salesStart != null && salesEnd != null && !salesStart.isBefore(salesEnd)) {
      throw new DomainValidationException("La venta debe empezar antes de terminar");
    }
    this.name = name.trim();
    this.priceCents = priceCents;
    this.currency = currency == null || currency.isBlank() ? "PEN" : currency.trim().toUpperCase();
    this.quantity = quantity;
    this.salesStart = salesStart;
    this.salesEnd = salesEnd;
  }

  public void moveTo(int position) {
    this.position = position;
  }

  public boolean isOnSale(ZonedDateTime now) {
    return (salesStart == null || !now.isBefore(salesStart))
        && (salesEnd == null || now.isBefore(salesEnd));
  }
}
