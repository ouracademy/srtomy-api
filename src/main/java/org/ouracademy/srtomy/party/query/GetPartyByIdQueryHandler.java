package org.ouracademy.srtomy.party.query;

import java.util.Optional;

public interface GetPartyByIdQueryHandler {
  Optional<GetPartyByIdResult> handle(GetPartyByIdQuery query);
}
