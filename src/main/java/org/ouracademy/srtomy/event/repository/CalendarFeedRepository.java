package org.ouracademy.srtomy.event.repository;

import java.util.Optional;
import org.ouracademy.srtomy.event.domain.CalendarFeed;
import org.ouracademy.srtomy.user.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarFeedRepository extends JpaRepository<CalendarFeed, UserId> {

  Optional<CalendarFeed> findByToken(String token);
}
