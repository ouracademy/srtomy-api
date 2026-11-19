package org.ouracademy.ovejafiel.billing.query.check_capacity;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.Plan;
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
    var max = people ? state.limits().maxPeople() : state.limits().maxLocations();
    if (max != null && used >= max) {
      throw new PlanLimitException(
          people ? "limit.people" : "limit.locations", message(state.plan(), people, max));
    }
    return new CheckCapacityResult(used, max);
  }

  /** What the plan includes, and which plan to move to (the next one), in Spanish. */
  private static String message(Plan plan, boolean people, int max) {
    var includes =
        people
            ? "hasta " + max + (max == 1 ? " persona" : " personas")
            : max + (max == 1 ? " sede" : " sedes");
    var next = plan.next().map(n -> " Pasa al plan " + n.label() + " para tener más.").orElse("");
    return "El plan " + plan.label() + " incluye " + includes + "." + next;
  }
}
