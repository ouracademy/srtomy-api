package org.ouracademy.srtomy.person.query;

import java.util.Optional;

public interface FindPersonByIdQueryHandler {
  /** Empty when no person has the given id (including when it is an organization's id). */
  Optional<FindPersonResult> handle(FindPersonByIdQuery query);
}
