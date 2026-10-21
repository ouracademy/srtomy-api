package org.ouracademy.srtomy.organization.query;

import java.util.List;

public interface GetMemberStateQueryHandler {
  List<MemberStateResult> handle(GetMemberStateQuery query);
}
