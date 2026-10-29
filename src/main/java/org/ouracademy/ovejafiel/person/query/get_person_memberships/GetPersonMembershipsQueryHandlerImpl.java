package org.ouracademy.ovejafiel.person.query.get_person_memberships;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQuery;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPersonMembershipsQueryHandlerImpl implements GetPersonMembershipsQueryHandler {

  private final GetPersonAccountabilitiesQueryHandler getPersonAccountabilitiesQueryHandler;
  private final FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public List<PersonMembershipResult> handle(GetPersonMembershipsQuery query) {
    var byNode = new LinkedHashMap<PartyId, List<PersonAccountabilityResult>>();
    for (var accountability :
        getPersonAccountabilitiesQueryHandler.handle(
            new GetPersonAccountabilitiesQuery(query.personId()))) {
      byNode
          .computeIfAbsent(accountability.organizationId(), id -> new ArrayList<>())
          .add(accountability);
    }
    var memberships = new ArrayList<PersonMembershipResult>();
    byNode.forEach(
        (organizationId, held) -> {
          // Only nodes the person is part of; a bare position or state elsewhere is not one.
          if (held.stream().noneMatch(a -> a.typeId().equals(IS_PART_OF))) {
            return;
          }
          findOrganizationByIdQueryHandler
              .handle(new FindOrganizationByIdQuery(organizationId))
              .ifPresent(
                  organization ->
                      memberships.add(
                          new PersonMembershipResult(
                              organizationId,
                              organization.name(),
                              organization.type(),
                              held.stream()
                                  .map(a -> stateOf(a.typeId()))
                                  .flatMap(Optional::stream)
                                  .findFirst()
                                  .orElse(null),
                              held.stream().anyMatch(a -> a.typeId().equals(LEADER)),
                              held.stream()
                                  .filter(a -> a.position() != null)
                                  .map(a -> a.position().name())
                                  .toList())));
        });
    memberships.sort(Comparator.comparing(PersonMembershipResult::organizationName));
    return memberships;
  }

  private static Optional<MemberState> stateOf(AccountabilityTypeId typeId) {
    return Arrays.stream(MemberState.values()).filter(s -> s.typeId().equals(typeId)).findFirst();
  }
}
