package org.ouracademy.ovejafiel.billing.query.check_capacity;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.query.check_capacity.CheckCapacityQuery.Resource;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsQuery;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsQueryHandler;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQuery;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.PlanLimitException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CheckCapacityQueryHandlerImpl implements CheckCapacityQueryHandler {

  private final Entitlements entitlements;
  private final BillingPolicy policy;
  private final CountPeopleQueryHandler countPeople;
  private final CountLocationsQueryHandler countLocations;

  @Override
  @Transactional(readOnly = true)
  public CheckCapacityResult handle(CheckCapacityQuery query) {
    var state = entitlements.of(query.organizationId());
    var people = query.resource() == Resource.PEOPLE;
    var used =
        people
            ? countPeople.handle(new CountPeopleQuery(state.churchId())).count()
            : countLocations.handle(new CountLocationsQuery(state.churchId())).count();
    if (state.isPaid()) {
      return new CheckCapacityResult(used, null);
    }
    var max = people ? policy.freeMaxPeople() : policy.freeMaxLocations();
    if (used >= max) {
      throw new PlanLimitException(
          people ? "limit.people" : "limit.locations",
          people
              ? "El plan gratuito incluye hasta "
                  + max
                  + " personas. Pasa al plan Iglesia para registrar más."
              : "El plan gratuito incluye "
                  + max
                  + (max == 1 ? " sede" : " sedes")
                  + ". Pasa al plan Iglesia para abrir más.");
    }
    return new CheckCapacityResult(used, max);
  }
}
