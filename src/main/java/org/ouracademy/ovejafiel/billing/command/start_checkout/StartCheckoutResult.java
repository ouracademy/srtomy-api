package org.ouracademy.ovejafiel.billing.command.start_checkout;

import java.math.BigDecimal;
import org.ouracademy.ovejafiel.billing.domain.InvoiceId;

/** {@code checkoutUrl}: where the browser goes to pay. */
public record StartCheckoutResult(
    InvoiceId invoiceId, String checkoutUrl, BigDecimal amount, String currency) {}
