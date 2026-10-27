package org.ouracademy.ovejafiel.event.repository;

import java.util.Optional;
import org.ouracademy.ovejafiel.event.domain.CalendarFeed;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarFeedRepository extends JpaRepository<CalendarFeed, UserId> {

  Optional<CalendarFeed> findByToken(String token);
}
