package org.ouracademy.ovejafiel.billing.query.list_invoices;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListInvoicesQueryHandlerImpl implements ListInvoicesQueryHandler {

  private final Entitlements entitlements;
  private final InvoiceRepository invoices;

  @Override
  @Transactional(readOnly = true)
  public List<InvoiceResult> handle(ListInvoicesQuery query) {
    var churchId = entitlements.of(query.organizationId()).churchId();
    return invoices.findByOrganizationIdOrderByCreateDateDesc(churchId).stream()
        .map(InvoiceResult::from)
        .toList();
  }
}
