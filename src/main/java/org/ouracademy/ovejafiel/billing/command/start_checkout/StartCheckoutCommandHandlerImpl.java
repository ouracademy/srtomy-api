package org.ouracademy.ovejafiel.billing.command.start_checkout;

import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.Invoice;
import org.ouracademy.ovejafiel.billing.gateway.PaymentGateway;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.repository.InvoiceRepository;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsQuery;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsQueryHandler;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQuery;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
class StartCheckoutCommandHandlerImpl implements StartCheckoutCommandHandler {

  private final Entitlements entitlements;
  private final BillingPolicy policy;
  private final CountPeopleQueryHandler countPeople;
  private final CountLocationsQueryHandler countLocations;
  private final InvoiceRepository invoices;
  private final PaymentGateway gateway;
  private final String appUrl;

  StartCheckoutCommandHandlerImpl(
      Entitlements entitlements,
      BillingPolicy policy,
      CountPeopleQueryHandler countPeople,
      CountLocationsQueryHandler countLocations,
      InvoiceRepository invoices,
      PaymentGateway gateway,
      @Value("${app.url:http://localhost:3000}") String appUrl) {
    this.entitlements = entitlements;
    this.policy = policy;
    this.countPeople = countPeople;
    this.countLocations = countLocations;
    this.invoices = invoices;
    this.gateway = gateway;
    this.appUrl = appUrl;
  }

  /**
   * The gateway is called inside the transaction on purpose: if it fails nothing is saved, and an
   * invoice whose checkout nobody opens is harmless.
   */
  @Override
  @Transactional
  public StartCheckoutResult handle(StartCheckoutCommand command) {
    var churchId = entitlements.of(command.organizationId()).churchId();
    var people = countPeople.handle(new CountPeopleQuery(churchId)).count();
    var quote =
        policy.quote(command.plan(), command.country(), command.months(), command.ownNumber());
    // A plan the church already outgrew would block it the day it paid: the next one is the answer.
    var limits = policy.limits(command.plan());
    var locations = countLocations.handle(new CountLocationsQuery(churchId)).count();
    if (!limits.allowsPeople(people) || !limits.allowsLocations(locations)) {
      throw new DomainValidationException(
          "Tu iglesia tiene "
              + people
              + " personas y "
              + locations
              + " sedes: el plan "
              + command.plan().label()
              + " no las incluye. Elige un plan más grande.");
    }
    var invoice =
        invoices.save(
            new Invoice(
                churchId,
                command.plan(),
                command.ownNumber(),
                command.country(),
                quote.total(),
                (int) people,
                command.months()));

    var back = appUrl.replaceAll("/+$", "") + "/portal/" + churchId.value() + "/billing?pago=";
    var payment =
        gateway.createPayment(
            new PaymentGateway.PaymentRequest(
                invoice.getId().value().toString(),
                quote.total(),
                quote.currency(),
                command.country().name(),
                "Sr. Tommy, plan "
                    + command.plan().label()
                    + (command.ownNumber() ? " con número propio" : "")
                    + ": "
                    + (command.months() == 12 ? "1 año" : "1 mes"),
                back + "completado",
                back + "cancelado"));
    invoice.sentToGateway(payment.id(), payment.checkoutUrl());
    log.info(
        "Checkout started: church={}, plan={}, invoice={}, {} {} for {} months and {} people",
        churchId.value(),
        command.plan(),
        invoice.getId().value(),
        quote.total(),
        quote.currency(),
        command.months(),
        people);
    return new StartCheckoutResult(
        invoice.getId(), payment.checkoutUrl(), quote.total(), quote.currency());
  }
}
