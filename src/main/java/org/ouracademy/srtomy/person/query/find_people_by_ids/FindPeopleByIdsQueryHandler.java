package org.ouracademy.srtomy.person.query.find_people_by_ids;

import java.util.List;
import org.ouracademy.srtomy.person.query.FindPersonResult;

public interface FindPeopleByIdsQueryHandler {
  /**
   * The people among the given ids, in no particular order. Ids that are not people (unknown ids,
   * organizations) are silently left out.
   */
  List<FindPersonResult> handle(FindPeopleByIdsQuery query);
}
