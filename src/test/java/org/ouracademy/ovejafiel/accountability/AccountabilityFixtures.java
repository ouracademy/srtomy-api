package org.ouracademy.ovejafiel.accountability;

import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.EntityId;
import org.springframework.test.util.ReflectionTestUtils;

/** Test helpers for the accountability module's handler tests. */
public final class AccountabilityFixtures {

  private AccountabilityFixtures() {}

  /** Minimal concrete {@link Party}: accountability only depends on the abstract type. */
  public static final class TestParty extends Party {}

  public static <T> T withId(T entity, EntityId id) {
    ReflectionTestUtils.setField(entity, "id", id);
    return entity;
  }

  public static Party party(PartyId id, String name) {
    var party = withId(new TestParty(), id);
    party.setName(name);
    return party;
  }
}
