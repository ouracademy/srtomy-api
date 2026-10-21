package org.ouracademy.srtomy.organization.query;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetMemberStateQueryHandlerImpl implements GetMemberStateQueryHandler {

  private final GetAccountabilitiesByParentAndTypesQueryHandler
      getAccountabilitiesByParentAndTypesQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public List<MemberStateResult> handle(GetMemberStateQuery query) {
    return getAccountabilitiesByParentAndTypesQueryHandler
        .handle(
            new GetAccountabilitiesByParentAndTypesQuery(
                query.organizationId(), List.of(MEMBER_PROGRESS_STATES)))
        .stream()
        .map(
            a ->
                new MemberStateResult(
                    a.id(), a.childId(), a.childName(), a.parentId(), a.typeId(), a.typeName()))
        .toList();
  }
}
