package org.ouracademy.ovejafiel.party.query.get_party_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.party.repository.PartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPartyByIdQueryHandlerImpl implements GetPartyByIdQueryHandler {

  private final PartyRepository partyRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetPartyByIdResult> handle(GetPartyByIdQuery query) {
    return partyRepository
        .findById(query.id())
        .map(party -> new GetPartyByIdResult(party.getId(), party.getName()));
  }
}
