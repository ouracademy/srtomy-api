package org.ouracademy.ovejafiel.accountability.command;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.MemberStateEvent;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Component;

/** Writes the history of how people join, progress in and leave nodes (see MemberStateEvent). */
@Component
@RequiredArgsConstructor
public class MemberStateEvents {

  private final MemberStateEventRepository repository;

  public void joined(PartyId personId, PartyId organizationId, String toState) {
    record(personId, organizationId, null, toState);
  }

  public void changed(PartyId personId, PartyId organizationId, String fromState, String toState) {
    record(personId, organizationId, fromState, toState);
  }

  public void left(PartyId personId, PartyId organizationId, String fromState) {
    record(personId, organizationId, fromState, null);
  }

  private void record(PartyId personId, PartyId organizationId, String from, String to) {
    // Setting the state someone already has is not an event.
    if (Objects.equals(from, to)) {
      return;
    }
    repository.save(new MemberStateEvent(personId, organizationId, from, to));
  }
}
