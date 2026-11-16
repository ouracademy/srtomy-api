package org.ouracademy.ovejafiel.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/** One payment of the paid plan: the period it covers and how the payment at the gateway went. */
@Entity
@Table(name = "billing_invoice")
@Getter
public class Invoice extends DomainObject {

  @Id InvoiceId id = InvoiceId.generate();

  @Column(nullable = false)
  PartyId organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  BillingCountry country;

  @Column(nullable = false)
  String currency;

  @Column(nullable = false)
  BigDecimal amount;

  /** People the church had when it was priced. */
  int people;

  int months;

  /** The period it pays for, known once paid: it starts where the paid time of the church ends. */
  LocalDate periodStart;

  LocalDate periodEnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  InvoiceStatus status = InvoiceStatus.PENDING;

  /** The id of the payment at the gateway; null until the gateway accepted the request. */
  String gatewayPaymentId;

  /** Where the church pays; only useful while pending. */
  @Column(columnDefinition = "text")
  String checkoutUrl;

  LocalDateTime paidAt;

  protected Invoice() {
    // jpa only
  }

  public Invoice(
      PartyId organizationId, BillingCountry country, BigDecimal amount, int people, int months) {
    this.organizationId = organizationId;
    this.country = country;
    this.currency = country.currency();
    this.amount = amount;
    this.people = people;
    this.months = months;
  }

  public void sentToGateway(String gatewayPaymentId, String checkoutUrl) {
    this.gatewayPaymentId = gatewayPaymentId;
    this.checkoutUrl = checkoutUrl;
  }

  public boolean isPending() {
    return status == InvoiceStatus.PENDING;
  }

  /**
   * Takes what the gateway says about the payment. Only a pending invoice changes: a paid one stays
   * paid whatever arrives later.
   *
   * @param periodStart the first day it covers if it turns out paid
   * @return whether it became paid just now
   */
  public boolean settle(InvoiceStatus gatewayStatus, LocalDateTime now, LocalDate periodStart) {
    if (!isPending() || gatewayStatus == InvoiceStatus.PENDING) {
      return false;
    }
    this.status = gatewayStatus;
    this.checkoutUrl = null;
    if (gatewayStatus == InvoiceStatus.PAID) {
      this.paidAt = now;
      this.periodStart = periodStart;
      this.periodEnd = periodStart.plusMonths(months).minusDays(1);
      return true;
    }
    return false;
  }
}
