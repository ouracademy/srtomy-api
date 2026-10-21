package org.ouracademy.srtomy.organization.query;

import java.util.List;
import java.util.Map;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Children grouped by accountability type name (e.g. "IS_PART_OF", "LEADER"). */
public record GetLocationChildrenResult(Map<String, List<Child>> childrenByType) {

  public record Child(PartyId id, String name, Kind kind) {}

  /** Parties are either organizations or people; anything not an organization is a person. */
  public enum Kind {
    ORGANIZATION,
    PERSON
  }
}
