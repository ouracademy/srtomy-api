package org.ouracademy.srtomy.organization.query.get_location_children;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.AccountabilityResult;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.ouracademy.srtomy.organization.query.PersonDetailsResolver;
import org.ouracademy.srtomy.organization.query.get_location_children.GetLocationChildrenResult.Child;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The members and leaders of a group or ministry. Sub-groups hang from their parent through {@code
 * organization.parent_id}, not through these accountabilities, so every child here is a person; the
 * details of all of them are fetched in one call through the person module's query handler.
 */
@Service
@RequiredArgsConstructor
class GetLocationChildrenQueryHandlerImpl implements GetLocationChildrenQueryHandler {

  private final GetAccountabilitiesByParentAndTypesQueryHandler
      getAccountabilitiesByParentAndTypesQueryHandler;
  private final PersonDetailsResolver personDetailsResolver;

  @Override
  @Transactional(readOnly = true)
  public GetLocationChildrenResult handle(GetLocationChildrenQuery query) {
    var accountabilities =
        getAccountabilitiesByParentAndTypesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(
                query.locationChildId(), List.of(IS_PART_OF, LEADER)));
    var people =
        personDetailsResolver.resolve(
            accountabilities.stream().map(AccountabilityResult::childId).toList());
    return new GetLocationChildrenResult(
        accountabilities.stream()
            .collect(
                groupingBy(
                    AccountabilityResult::typeName,
                    mapping(
                        a -> new Child(a.childId(), a.childName(), people.get(a.childId())),
                        toList()))));
  }
}
