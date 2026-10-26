package org.ouracademy.ovejafiel.party.query.get_party_by_id;

import java.util.Optional;

public interface GetPartyByIdQueryHandler {
  Optional<GetPartyByIdResult> handle(GetPartyByIdQuery query);
}
