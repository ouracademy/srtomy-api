package org.ouracademy.srtomy.person.query.find_person_by_id;

import java.util.Optional;
import org.ouracademy.srtomy.person.query.FindPersonResult;

public interface FindPersonByIdQueryHandler {
  /** Empty when no person has the given id (including when it is an organization's id). */
  Optional<FindPersonResult> handle(FindPersonByIdQuery query);
}
