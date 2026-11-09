package org.ouracademy.ovejafiel.event.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRegistrationRepository
    extends JpaRepository<EventRegistration, EventRegistrationId> {

  List<EventRegistration> findByEventIdAndStatusNotOrderByCreateDate(
      EventId eventId, RegistrationStatus status);

  Optional<EventRegistration> findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
      EventId eventId, String email, RegistrationStatus status);

  Optional<EventRegistration> findFirstByEventIdAndUserIdAndStatusNot(
      EventId eventId, UserId userId, RegistrationStatus status);

  Optional<EventRegistration> findByCancelCode(String cancelCode);

  Optional<EventRegistration> findByTicketCode(String ticketCode);

  List<EventRegistration> findByEventIdInAndStatusNot(
      Collection<EventId> eventIds, RegistrationStatus status);

  boolean existsByTicketTypeId(TicketTypeId ticketTypeId);

  boolean existsByEventIdAndStatusNot(EventId eventId, RegistrationStatus status);

  long countByEventIdAndStatusNot(EventId eventId, RegistrationStatus status);

  long countByEventIdAndStatus(EventId eventId, RegistrationStatus status);

  @Query(
      "select count(r) from EventRegistration r "
          + "where r.event.id = :eventId and r.checkedInAt is not null")
  long countCheckedIn(EventId eventId);

  /**
   * The sign-ups to remind of an event that starts in the window: with a seat, with a phone and not
   * yet checked in. The event query hides the deleted ones.
   */
  @Query(
      "select r from EventRegistration r join fetch r.event e "
          + "where r.status = org.ouracademy.ovejafiel.event.domain.RegistrationStatus.CONFIRMED "
          + "and r.checkedInAt is null and r.phoneNumber is not null and r.phoneNumber <> '' "
          + "and e.start >= :from and e.start < :to order by e.start, r.createDate")
  List<EventRegistration> findToRemind(ZonedDateTime from, ZonedDateTime to);

  void deleteByEventId(EventId eventId);

  /** Rows of {@code [ticket type id, how many]} for the sign-ups in {@code status}. */
  @Query(
      "select r.ticketType.id, count(r) from EventRegistration r "
          + "where r.event.id = :eventId and r.status = :status group by r.ticketType.id")
  List<Object[]> countByTicketType(EventId eventId, RegistrationStatus status);
}
