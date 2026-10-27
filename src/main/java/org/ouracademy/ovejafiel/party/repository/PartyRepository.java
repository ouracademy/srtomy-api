package org.ouracademy.ovejafiel.party.repository;

import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Internal to the party module. Other modules must not import this repository; they reference a
 * Party through {@code EntityManager.getReference(Party.class, id)} or query it through {@link
 * org.ouracademy.ovejafiel.party.query.get_party_by_id.GetPartyByIdQueryHandler}.
 */
public interface PartyRepository extends JpaRepository<Party, PartyId> {}
