package org.ouracademy.ovejafiel.billing.command.remind_renewals;

public interface RemindRenewalsCommandHandler {
  RemindRenewalsResult handle(RemindRenewalsCommand command);
}
