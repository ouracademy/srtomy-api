package org.ouracademy.ovejafiel.billing.gateway;

import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;

/**
 * Stand-in for local runs and tests, when no gateway key is configured: nobody is charged. The
 * "checkout" is the success page of the app itself and every payment counts as paid, so the whole
 * flow can be tried without an account. It accepts no notification.
 */
@Slf4j
class SandboxPaymentGateway implements PaymentGateway {

  private static final String PREFIX = "SANDBOX-";

  @Override
  public CreatedPayment createPayment(PaymentRequest request) {
    log.info(
        "Payment of {} {} for {} (not charged, no gateway configured)",
        request.amount(),
        request.currency(),
        request.orderId());
    return new CreatedPayment(PREFIX + request.orderId(), request.successUrl());
  }

  @Override
  public PaymentState findPayment(String paymentId) {
    return new PaymentState(paymentId, paymentId.substring(PREFIX.length()), InvoiceStatus.PAID);
  }

  @Override
  public boolean isAuthentic(byte[] rawBody, String authorization) {
    return false;
  }
}
