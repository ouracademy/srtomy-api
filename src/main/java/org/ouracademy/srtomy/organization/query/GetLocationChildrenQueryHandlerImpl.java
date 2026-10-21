package org.ouracademy.srtomy.organization.query;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.AccountabilityResult;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.GetLocationChildrenResult.Child;
import org.ouracademy.srtomy.organization.query.GetLocationChildrenResult.Kind;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Types each child by checking it against this module's own organizations. */
@Service
@RequiredArgsConstructor
class GetLocationChildrenQueryHandlerImpl implements GetLocationChildrenQueryHandler {

  private final GetAccountabilitiesByParentAndTypesQueryHandler
      getAccountabilitiesByParentAndTypesQueryHandler;
  private final OrganizationRepository organizationRepository;

  @Override
  @Transactional(readOnly = true)
  public GetLocationChildrenResult handle(GetLocationChildrenQuery query) {
    var accountabilities =
        getAccountabilitiesByParentAndTypesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(
                query.locationChildId(), List.of(IS_PART_OF, LEADER)));
    var childIds = accountabilities.stream().map(AccountabilityResult::childId).toList();
    var organizationIds =
        childIds.isEmpty()
            ? Set.<PartyId>of()
            : organizationRepository.findAllById(childIds).stream()
                .map(Organization::getId)
                .collect(toSet());
    return new GetLocationChildrenResult(
        accountabilities.stream()
            .collect(
                groupingBy(
                    AccountabilityResult::typeName,
                    mapping(
                        a ->
                            new Child(
                                a.childId(),
                                a.childName(),
                                organizationIds.contains(a.childId())
                                    ? Kind.ORGANIZATION
                                    : Kind.PERSON),
                        toList()))));
  }
}
