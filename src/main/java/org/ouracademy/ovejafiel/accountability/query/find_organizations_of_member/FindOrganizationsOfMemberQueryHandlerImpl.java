package org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindOrganizationsOfMemberQueryHandlerImpl implements FindOrganizationsOfMemberQueryHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public OrganizationsOfMemberResult handle(FindOrganizationsOfMemberQuery query) {
    return new OrganizationsOfMemberResult(
        accountabilityRepository.findByChildIdAndTypeId(query.memberId(), IS_PART_OF).stream()
            .map(accountability -> accountability.getParent().getId())
            .toList());
  }
}
