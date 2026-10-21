package org.ouracademy.srtomy.organization.query;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.AccountabilityResult;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQueryHandler;
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
