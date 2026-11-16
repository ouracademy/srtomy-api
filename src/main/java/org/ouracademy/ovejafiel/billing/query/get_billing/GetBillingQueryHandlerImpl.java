package org.ouracademy.ovejafiel.billing.query.get_billing;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.query.AiAllowance;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.AiResult;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.FeatureState;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult.PriceResult;
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
    var paid = state.isPaid();
    var ai = aiAllowance.of(state, people);
    return new GetBillingResult(
        state.churchId(),
        paid ? Plan.CHURCH : Plan.FREE,
        state.status(),
        subscription == null ? null : subscription.getPaidUntil(),
        subscription == null ? null : subscription.getCountry(),
        people,
        countLocations.handle(new CountLocationsQuery(state.churchId())).count(),
        paid ? null : policy.freeMaxPeople(),
        paid ? null : policy.freeMaxLocations(),
        Arrays.stream(Feature.values())
            .map(f -> new FeatureState(f, f.label(), state.allows(f)))
            .toList(),
        state.whatsappTrialEndsAt(),
        policy.whatsappTrialDays(),
        policy.includedPeople(),
        policy.blockSize(),
        Arrays.stream(BillingCountry.values())
            .filter(policy.prices()::containsKey)
            .map(
                country ->
                    new PriceResult(
                        country,
                        country.currency(),
                        policy.quote(country, people, 1).total(),
                        policy.quote(country, people, 12).total(),
                        policy.prices().get(country).extraBlock()))
            .toList(),
        new AiResult(ai.month(), ai.used(), ai.limit(), ai.costUsd(), ai.maxCostUsd()));
  }
}
