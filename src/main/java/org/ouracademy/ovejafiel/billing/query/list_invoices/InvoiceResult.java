package org.ouracademy.ovejafiel.billing.query.list_invoices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.billing.domain.Invoice;
import org.ouracademy.ovejafiel.billing.domain.InvoiceId;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;

/**
 * @param checkoutUrl where to finish paying; null unless pending
 */
public record InvoiceResult(
    InvoiceId id,
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

  public static InvoiceResult from(Invoice invoice) {
    return new InvoiceResult(
        invoice.getId(),
        invoice.getStatus(),
        invoice.getAmount(),
        invoice.getCurrency(),
        invoice.getMonths(),
        invoice.getPeople(),
        invoice.getPeriodStart(),
        invoice.getPeriodEnd(),
        invoice.getCreateDate(),
        invoice.getPaidAt(),
        invoice.getCheckoutUrl());
  }
}
