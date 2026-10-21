package org.ouracademy.srtomy.organization.query.get_member_state;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.AccountabilityResult;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.ouracademy.srtomy.organization.query.PersonDetailsResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetMemberStateQueryHandlerImpl implements GetMemberStateQueryHandler {

  private final GetAccountabilitiesByParentAndTypesQueryHandler
      getAccountabilitiesByParentAndTypesQueryHandler;
  private final PersonDetailsResolver personDetailsResolver;

  @Override
  @Transactional(readOnly = true)
  public List<MemberStateResult> handle(GetMemberStateQuery query) {
    var accountabilities =
        getAccountabilitiesByParentAndTypesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(
                query.organizationId(), List.of(MEMBER_PROGRESS_STATES)));
    var people =
        personDetailsResolver.resolve(
            accountabilities.stream().map(AccountabilityResult::childId).toList());
    return accountabilities.stream()
        .map(
            a ->
                new MemberStateResult(
                    a.id(),
                    a.childId(),
                    a.childName(),
                    a.parentId(),
                    a.typeId(),
                    a.typeName(),
                    people.get(a.childId())))
        .toList();
  }
}
