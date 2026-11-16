package org.ouracademy.ovejafiel.billing.command.sync_invoices;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.command.InvoiceSettler;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SyncInvoicesCommandHandlerImpl implements SyncInvoicesCommandHandler {

  private final Entitlements entitlements;
  private final InvoiceRepository invoices;
  private final InvoiceSettler settler;

  @Override
  @Transactional
  public SyncInvoicesResult handle(SyncInvoicesCommand command) {
    var churchId = entitlements.of(command.organizationId()).churchId();
    var paid = 0;
    for (var invoice : invoices.findByOrganizationIdAndStatus(churchId, InvoiceStatus.PENDING)) {
      if (settler.settle(invoice)) {
        paid++;
      }
    }
    return new SyncInvoicesResult(paid);
  }
}
