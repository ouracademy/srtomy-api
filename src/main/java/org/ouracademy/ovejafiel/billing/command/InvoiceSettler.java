package org.ouracademy.ovejafiel.billing.command;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.billing.domain.Invoice;
import org.ouracademy.ovejafiel.billing.domain.InvoiceId;
import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.billing.gateway.PaymentGateway;
import org.ouracademy.ovejafiel.billing.repository.InvoiceRepository;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;

/**
 * Brings an invoice up to date with the gateway and, when it turns out paid, extends the paid time
 * of the church. What counts is what the gateway answers when asked, never what a notification
 * says. Must run inside a transaction: the invoice is locked so two checks of the same payment
 * cannot both extend the plan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceSettler {

  private final InvoiceRepository invoices;
  private final SubscriptionRepository subscriptions;
  private final PaymentGateway gateway;
  private final Clock clock;

  /**
   * @return whether the invoice became paid just now
   */
  public boolean settle(InvoiceId invoiceId) {
    var invoice = invoices.findForUpdate(invoiceId).orElse(null);
    if (invoice == null || !invoice.isPending() || invoice.getGatewayPaymentId() == null) {
      return false;
    }
    var payment = gateway.findPayment(invoice.getGatewayPaymentId());
    if (payment.orderId() != null && !payment.orderId().equals(invoiceId.value().toString())) {
      // The payment is of something else: never credit it to this church.
      log.warn(
          "Payment {} is of order {}, not of invoice {}: ignored",
          payment.id(),
          payment.orderId(),
          invoiceId.value());
      return false;
    }
    var subscription =
        subscriptions
            .findById(invoice.getOrganizationId())
            .orElseGet(() -> new Subscription(invoice.getOrganizationId()));
    var paid =
        invoice.settle(
            payment.status(),
            LocalDateTime.now(clock),
            subscription.nextPeriodStart(LocalDate.now(clock)));
    if (paid) {
      subscription.extendTo(
          invoice.getPeriodEnd(), invoice.getCountry(), invoice.getPlan(), invoice.isOwnNumber());
      subscriptions.save(subscription);
      log.info(
          "Invoice {} paid: church {} has the plan {} until {}",
          invoiceId.value(),
          invoice.getOrganizationId().value(),
          invoice.getPlan(),
          subscription.getPaidUntil());
    }
    invoices.save(invoice);
    return paid;
  }

  /** For callers that only hold the invoice. */
  public boolean settle(Invoice invoice) {
    return settle(invoice.getId());
  }
}
