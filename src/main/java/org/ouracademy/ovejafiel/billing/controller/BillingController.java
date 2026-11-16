package org.ouracademy.ovejafiel.billing.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.billing.command.start_checkout.StartCheckoutCommand;
import org.ouracademy.ovejafiel.billing.command.start_checkout.StartCheckoutCommandHandler;
import org.ouracademy.ovejafiel.billing.command.sync_invoices.SyncInvoicesCommand;
import org.ouracademy.ovejafiel.billing.command.sync_invoices.SyncInvoicesCommandHandler;
import org.ouracademy.ovejafiel.billing.controller.dto.BillingResponse;
import org.ouracademy.ovejafiel.billing.controller.dto.CheckoutRequest;
import org.ouracademy.ovejafiel.billing.controller.dto.CheckoutResponse;
import org.ouracademy.ovejafiel.billing.controller.dto.InvoiceResponse;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingQuery;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingQueryHandler;
import org.ouracademy.ovejafiel.billing.query.list_invoices.ListInvoicesQuery;
import org.ouracademy.ovejafiel.billing.query.list_invoices.ListInvoicesQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The plan of a church. Anyone who can see the node ({@code org.read}) can see the plan, because
 * the web app needs it to tell what is locked; paying and the list of payments need {@code
 * billing.manage}, which the owner and the administrators hold.
 */
@RestController
@AllArgsConstructor
@Tag(name = "billing")
public class BillingController {

  GetBillingQueryHandler getBillingQueryHandler;
  ListInvoicesQueryHandler listInvoicesQueryHandler;
  StartCheckoutCommandHandler startCheckoutCommandHandler;
  SyncInvoicesCommandHandler syncInvoicesCommandHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  /**
   * The plan of the church: FREE or CHURCH, its status, until when it is paid, how many people and
   * locations it uses against the maximum of the plan, which features it includes, the end of the
   * WhatsApp trial and what the paid plan costs today in each country. Needs {@code org.read}.
   *
   * @param id Id of the node: a church, location, group or ministry. The plan is the church's.
   */
  @GetMapping("organizations/{id}/billing")
  public BillingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var node = new PartyId(id);
    authorize(jwt, node, Permission.ORG_READ);
    return BillingResponse.from(getBillingQueryHandler.handle(new GetBillingQuery(node)));
  }

  /**
   * The payments of the church, the newest first, with the period each one paid for. Needs {@code
   * billing.manage}.
   *
   * @param id Id of the node: a church, location, group or ministry. The plan is the church's.
   */
  @GetMapping("organizations/{id}/billing/invoices")
  public List<InvoiceResponse> invoices(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var node = new PartyId(id);
    authorize(jwt, node, Permission.BILLING_MANAGE);
    return listInvoicesQueryHandler.handle(new ListInvoicesQuery(node)).stream()
        .map(InvoiceResponse::from)
        .toList();
  }

  /**
   * Starts paying the CHURCH plan for one month or one year, priced for the people the church has
   * today, and answers with the link of the checkout where the pastor pays. Nothing changes until
   * the payment is confirmed. Needs {@code billing.manage}.
   *
   * @param id Id of the node: a church, location, group or ministry. The plan is the church's.
   */
  @PostMapping("organizations/{id}/billing/checkout")
  @ResponseStatus(HttpStatus.CREATED)
  public CheckoutResponse checkout(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid CheckoutRequest request) {
    var node = new PartyId(id);
    authorize(jwt, node, Permission.BILLING_MANAGE);
    var result =
        startCheckoutCommandHandler.handle(
            new StartCheckoutCommand(node, request.country(), request.months()));
    return new CheckoutResponse(
        result.invoiceId().value(), result.checkoutUrl(), result.amount(), result.currency());
  }

  /**
   * Checks the pending payments of the church with the payment provider and answers with the plan
   * as it is after that. Called when the pastor comes back from the checkout. Needs {@code
   * billing.manage}.
   *
   * @param id Id of the node: a church, location, group or ministry. The plan is the church's.
   */
  @PostMapping("organizations/{id}/billing/sync")
  public BillingResponse sync(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var node = new PartyId(id);
    authorize(jwt, node, Permission.BILLING_MANAGE);
    syncInvoicesCommandHandler.handle(new SyncInvoicesCommand(node));
    return BillingResponse.from(getBillingQueryHandler.handle(new GetBillingQuery(node)));
  }

  private void authorize(Jwt jwt, PartyId node, Permission permission) {
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), node, permission));
  }
}
