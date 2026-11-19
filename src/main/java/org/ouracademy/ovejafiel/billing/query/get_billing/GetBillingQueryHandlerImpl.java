package org.ouracademy.ovejafiel.billing.query.get_billing;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.query.AiAllowance;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.AiResult;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.FeatureState;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.PlanResult;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.PriceResult;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.RemindersResult;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsQuery;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsQueryHandler;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQuery;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetBillingQueryHandlerImpl implements GetBillingQueryHandler {

  private final Entitlements entitlements;
  private final BillingPolicy policy;
  private final CountPeopleQueryHandler countPeople;
  private final CountLocationsQueryHandler countLocations;
  private final AiAllowance aiAllowance;

  @Override
  @Transactional(readOnly = true)
  public GetBillingResult handle(GetBillingQuery query) {
    var state = entitlements.of(query.organizationId());
    var subscription = state.subscription();
    var people = countPeople.handle(new CountPeopleQuery(state.churchId())).count();
    var locations = countLocations.handle(new CountLocationsQuery(state.churchId())).count();
    var ai = aiAllowance.of(state);
    var limits = state.limits();
    return new GetBillingResult(
        state.churchId(),
        state.plan(),
        state.status(),
        subscription == null ? null : subscription.getPaidUntil(),
        subscription == null ? null : subscription.getCountry(),
        state.allows(Feature.OWN_NUMBER),
        people,
        locations,
        limits.maxPeople(),
        limits.maxLocations(),
        Arrays.stream(Feature.values())
            .map(f -> new FeatureState(f, f.label(), state.allows(f)))
            .toList(),
        Arrays.stream(Plan.values()).map(plan -> planResult(plan, people, locations)).toList(),
        new AiResult(ai.month(), ai.used(), ai.limit(), ai.costUsd(), ai.maxCostUsd()),
        new RemindersResult(ai.remindersUsed(), ai.remindersLimit()));
  }

  private PlanResult planResult(Plan plan, long people, long locations) {
    var l = policy.limits(plan);
    return new PlanResult(
        plan,
        plan.label(),
        l.maxPeople(),
        l.maxLocations(),
        l.aiMessages(),
        l.whatsappReminders(),
        l.ownNumberIncluded(),
        l.ownNumberExtra(),
        l.customRoles(),
        l.excelExport(),
        l.churchFeatures(),
        l.allowsPeople(people) && l.allowsLocations(locations),
        plan.isPaid() ? prices(plan) : List.of());
  }

  private List<PriceResult> prices(Plan plan) {
    return Arrays.stream(BillingCountry.values())
        .map(
            country -> {
              var monthly = policy.monthlyPrice(plan, country);
              var own = policy.ownNumberMonthly(country);
              var months = BigDecimal.valueOf(policy.yearlyMonthsCharged());
              return new PriceResult(
                  country,
                  country.currency(),
                  monthly,
                  monthly.multiply(months),
                  own,
                  own.multiply(months));
            })
        .toList();
  }
}
