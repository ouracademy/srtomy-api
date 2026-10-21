package org.ouracademy.srtomy.person.query.search_people;

import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.domain.Pageable;

/**
 * The people that are IS_PART_OF {@code organizationId}, optionally filtered by {@code searchText}
 * (case-insensitive, on first or last name). A null {@code searchText} means no filter.
 */
public record SearchPeopleQuery(PartyId organizationId, String searchText, Pageable pageable) {}
