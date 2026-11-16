package org.ouracademy.ovejafiel.billing.command.sync_invoices;

/**
 * Used when the church comes back from the checkout: the notification of the gateway may not have
 * arrived yet (or cannot arrive, on a local run).
 */
public interface SyncInvoicesCommandHandler {
  SyncInvoicesResult handle(SyncInvoicesCommand command);
}
