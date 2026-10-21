package org.ouracademy.srtomy.accountability.query.get_roles_of_location;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQuery;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQueryHandler;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetRolesOfLocationQueryHandlerImpl implements GetRolesOfLocationQueryHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final GetPredefinedRolesQueryHandler getPredefinedRolesQueryHandler;
  private final EntityManager entityManager;

  @Override
  @Transactional(readOnly = true)
  public List<AccountabilityTypeResult> handle(GetRolesOfLocationQuery query) {
    var rolesOfLocation =
        accountabilityTypeRepository
            .findByParty(entityManager.getReference(Party.class, query.locationId()))
            .stream()
            .map(AccountabilityTypeResult::from);
    var predefinedRoles = getPredefinedRolesQueryHandler.handle(new GetPredefinedRolesQuery());

    return Stream.concat(rolesOfLocation, predefinedRoles.stream()).toList();
  }
}
