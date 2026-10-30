package org.ouracademy.ovejafiel.person.query.search_people;

import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.springframework.data.domain.Pageable;

/**
 * The people that are IS_PART_OF {@code organizationId} (or any node below it), optionally filtered
 * by {@code searchText} (case-insensitive, on first or last name) and by {@code state} (their state
 * in {@code organizationId} itself). A null filter means no filter.
 */
public record SearchPeopleQuery(
    PartyId organizationId, String searchText, MemberState state, Pageable pageable) {

  public SearchPeopleQuery(PartyId organizationId, String searchText, Pageable pageable) {
    this(organizationId, searchText, null, pageable);
  }
}
