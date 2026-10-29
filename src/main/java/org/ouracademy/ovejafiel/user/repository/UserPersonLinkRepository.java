package org.ouracademy.ovejafiel.user.repository;

import java.util.Optional;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.domain.UserPersonLink;
import org.ouracademy.ovejafiel.user.domain.UserPersonLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPersonLinkRepository extends JpaRepository<UserPersonLink, UserPersonLinkId> {

  Optional<UserPersonLink> findByUserIdAndChurchId(UserId userId, PartyId churchId);

  boolean existsByPersonId(PartyId personId);

  long deleteByPersonId(PartyId personId);

  boolean existsByUserIdAndChurchId(UserId userId, PartyId churchId);
}
