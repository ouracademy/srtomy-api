package org.ouracademy.srtomy.iam.query.list_access;

import java.util.List;

public interface ListAccessQueryHandler {
  List<AccessEntryResult> handle(ListAccessQuery query);
}
