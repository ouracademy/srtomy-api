package org.ouracademy.srtomy.organization.query.get_location_children;

import java.util.List;
import java.util.Map;
import org.ouracademy.srtomy.organization.query.PersonDetails;
import org.ouracademy.srtomy.party.domain.PartyId;

/** People of a group or ministry, grouped by accountability type name ("IS_PART_OF", "LEADER"). */
public record GetLocationChildrenResult(Map<String, List<Child>> childrenByType) {

  /** {@code person} is null when the id does not resolve to a person. */
  public record Child(PartyId id, String name, PersonDetails person) {}
}
