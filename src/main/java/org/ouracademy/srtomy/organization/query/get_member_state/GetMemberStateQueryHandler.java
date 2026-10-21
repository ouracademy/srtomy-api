package org.ouracademy.srtomy.organization.query.get_member_state;

import java.util.List;

public interface GetMemberStateQueryHandler {
  List<MemberStateResult> handle(GetMemberStateQuery query);
}
