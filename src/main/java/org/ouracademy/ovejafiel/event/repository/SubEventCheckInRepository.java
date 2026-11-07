package org.ouracademy.ovejafiel.event.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.SubEventCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubEventCheckInRepository extends JpaRepository<SubEventCheckIn, UUID> {

  Optional<SubEventCheckIn> findByRegistrationIdAndEventId(
      EventRegistrationId registrationId, EventId eventId);

  List<SubEventCheckIn> findByEventId(EventId eventId);
}
