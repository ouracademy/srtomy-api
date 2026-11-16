package org.ouracademy.ovejafiel.billing.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;
import org.ouracademy.ovejafiel.billing.query.list_invoices.InvoiceResult;

public record InvoiceResponse(
    UUID id,
    InvoiceStatus status,
    BigDecimal amount,
    String currency,
    int months,
    int people,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime createdAt,
    LocalDateTime paidAt,
    String checkoutUrl) {

  public static InvoiceResponse from(InvoiceResult result) {
    return new InvoiceResponse(
        result.id().value(),
        result.status(),
        result.amount(),
        result.currency(),
        result.months(),
        result.people(),
        result.periodStart(),
        result.periodEnd(),
        result.createdAt(),
        result.paidAt(),
        result.checkoutUrl());
  }
}
