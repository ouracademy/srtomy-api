package org.ouracademy.srtomy.user.repository;

import java.util.Optional;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.domain.UserPersonLink;
import org.ouracademy.srtomy.user.domain.UserPersonLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPersonLinkRepository extends JpaRepository<UserPersonLink, UserPersonLinkId> {

  Optional<UserPersonLink> findByUserIdAndChurchId(UserId userId, PartyId churchId);

  boolean existsByPersonId(PartyId personId);

  boolean existsByUserIdAndChurchId(UserId userId, PartyId churchId);
}
