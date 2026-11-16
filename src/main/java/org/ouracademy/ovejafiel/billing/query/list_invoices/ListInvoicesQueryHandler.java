package org.ouracademy.ovejafiel.billing.query.list_invoices;

import java.util.List;

public interface ListInvoicesQueryHandler {
  List<InvoiceResult> handle(ListInvoicesQuery query);
}
