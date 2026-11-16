package org.ouracademy.ovejafiel.billing.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.billing.command.remind_renewals.RemindRenewalsCommand;
import org.ouracademy.ovejafiel.billing.command.remind_renewals.RemindRenewalsCommandHandler;
import org.ouracademy.ovejafiel.billing.command.remind_renewals.RemindRenewalsResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called once a day by Cloud Scheduler (job {@code billing-renewal-reminders}), never by the web
 * app: only the scheduler's service account may call it (see {@code InternalSecurityConfig}).
 */
@RestController
@AllArgsConstructor
@Hidden
public class BillingJobsController {

  RemindRenewalsCommandHandler remindRenewalsCommandHandler;

  @PostMapping("internal/billing/remind")
  public RemindRenewalsResult remind() {
    return remindRenewalsCommandHandler.handle(new RemindRenewalsCommand());
  }
}
