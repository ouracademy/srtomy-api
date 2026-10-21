package org.ouracademy.srtomy.person.query;

import java.util.List;

public interface FindPeopleByIdsQueryHandler {
  /**
   * The people among the given ids, in no particular order. Ids that are not people (unknown ids,
   * organizations) are silently left out.
   */
  List<FindPersonResult> handle(FindPeopleByIdsQuery query);
}
