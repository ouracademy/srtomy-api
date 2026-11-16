package org.ouracademy.ovejafiel.billing.command.sync_payment;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.command.InvoiceSettler;
import org.ouracademy.ovejafiel.billing.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SyncPaymentCommandHandlerImpl implements SyncPaymentCommandHandler {

  private final InvoiceRepository invoices;
  private final InvoiceSettler settler;

  @Override
  @Transactional
  public SyncPaymentResult handle(SyncPaymentCommand command) {
    return invoices
        .findByGatewayPaymentId(command.gatewayPaymentId())
        .map(invoice -> new SyncPaymentResult(true, settler.settle(invoice)))
        .orElseGet(() -> new SyncPaymentResult(false, false));
  }
}
