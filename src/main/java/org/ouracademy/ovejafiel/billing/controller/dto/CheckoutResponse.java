package org.ouracademy.ovejafiel.billing.controller.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponse(
    UUID invoiceId, String checkoutUrl, BigDecimal amount, String currency) {}
