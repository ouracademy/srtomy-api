package org.ouracademy.srtomy.party.repository;

import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Internal to the party module. Other modules must not import this repository; they reference a
 * Party through {@code EntityManager.getReference(Party.class, id)} or query it through {@link
 * org.ouracademy.srtomy.party.query.GetPartyByIdQueryHandler}.
 */
public interface PartyRepository extends JpaRepository<Party, PartyId> {}
