package org.ouracademy.ovejafiel.accountability.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.IdGenerator;

/**
 * One step in how a person relates to a node: they joined (no {@code fromState}), changed state
 * (visitor, attendee, member) or left (no {@code toState}). The states are the names of the system
 * accountability types. It is history: rows are never changed.
 */
@Entity
@Getter
public class MemberStateEvent {

  @Id UUID id = IdGenerator.newUuidV7();

  PartyId personId;

  PartyId organizationId;

  String fromState;

  String toState;

  LocalDateTime occurredAt = LocalDateTime.now();

  MemberStateEvent() {
    // jpa only
  }

  public MemberStateEvent(
      PartyId personId, PartyId organizationId, String fromState, String toState) {
    this.personId = personId;
    this.organizationId = organizationId;
    this.fromState = fromState;
    this.toState = toState;
  }
}
