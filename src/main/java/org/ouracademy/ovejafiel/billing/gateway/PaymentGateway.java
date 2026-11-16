package org.ouracademy.ovejafiel.billing.gateway;

import java.math.BigDecimal;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;

/** The company that charges the church: it shows the checkout and tells us how the payment went. */
public interface PaymentGateway {

  /**
   * @param orderId our id of what is paid (the invoice); the gateway gives it back with the payment
   */
  record PaymentRequest(
      String orderId,
      BigDecimal amount,
      String currency,
      String country,
      String description,
      String successUrl,
      String backUrl) {}

  /** {@code checkoutUrl}: where the church is sent to pay. */
  record CreatedPayment(String id, String checkoutUrl) {}

  record PaymentState(String id, String orderId, InvoiceStatus status) {}

  CreatedPayment createPayment(PaymentRequest request);

  /** Asks the gateway for the payment: the answer, not the notification, is what we believe. */
  PaymentState findPayment(String paymentId);

  /**
   * Whether a notification really comes from the gateway.
   *
   * @param rawBody the bytes received, never a re-serialized object
   * @param authorization the {@code Authorization} header of the notification
   */
  boolean isAuthentic(byte[] rawBody, String authorization);
}
