package org.ouracademy.ovejafiel.attendance.command.mark_self_attendance;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommand;
import org.ouracademy.ovejafiel.attendance.command.add_visitor.AddVisitorCommandHandler;
import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceResult.Kind;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_user_present.MarkUserPresentCommandHandler;
import org.ouracademy.ovejafiel.attendance.query.AttendanceQrToken;
import org.ouracademy.ovejafiel.attendance.query.AttendanceWindow;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.command.check_in_user_registration.CheckInUserRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.check_in_user_registration.CheckInUserRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class MarkSelfAttendanceCommandHandlerImpl implements MarkSelfAttendanceCommandHandler {

  private final AttendanceQrToken attendanceQrToken;
  private final FindEventByIdQueryHandler findEventByIdQueryHandler;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final MarkUserPresentCommandHandler markUserPresentCommandHandler;
  private final CheckInUserRegistrationCommandHandler checkInUserRegistrationCommandHandler;
  private final AddVisitorCommandHandler addVisitorCommandHandler;
  private final EventAttendanceRepository eventAttendanceRepository;

  @Override
  @Transactional
  public MarkSelfAttendanceResult handle(MarkSelfAttendanceCommand command) {
    var verified =
        attendanceQrToken
            .verify(command.token(), Instant.now())
            .orElseThrow(() -> new NoSuchElementException("Attendance code not found"));
    if (!verified.fresh()) {
      throw new DomainValidationException("El código venció: pida al ujier que muestre el nuevo");
    }
    var event =
        findEventByIdQueryHandler
            .handle(new FindEventByIdQuery(verified.eventId()))
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    if (!AttendanceWindow.isOpen(event.start(), event.end(), ZonedDateTime.now())) {
      throw new DomainValidationException("Este evento no está tomando asistencia ahora");
    }

    var user = command.subject() == null ? null : findUser(command.subject());
    var name = command.name() == null ? "" : command.name().trim();
    if (user != null) {
      var registration =
          checkInUserRegistrationCommandHandler.handle(
              new CheckInUserRegistrationCommand(
                  verified.eventId(), user.id(), user.emailVerified() ? user.email() : null));
      var person =
          markUserPresentCommandHandler.handle(
              new MarkUserPresentCommand(
                  verified.eventId(), user.id(), CheckInSource.QR_EVENT, null));
      if (person.isPresent()) {
        return new MarkSelfAttendanceResult(Kind.PERSON, person.get().alreadyMarked());
      }
      if (registration.isPresent()) {
        return new MarkSelfAttendanceResult(
            Kind.REGISTRATION, registration.get().alreadyCheckedIn());
      }
    }
    if (!name.isEmpty()) {
      addVisitorCommandHandler.handle(
          new AddVisitorCommand(
              null,
              verified.eventId(),
              visitorInfo(name),
              CheckInSource.QR_EVENT,
              null,
              user == null ? null : user.id()));
      return new MarkSelfAttendanceResult(Kind.VISITOR, false);
    }
    eventAttendanceRepository.createIfMissing(verified.eventId().value());
    eventAttendanceRepository.addGuests(verified.eventId(), 1);
    return new MarkSelfAttendanceResult(Kind.ANONYMOUS, false);
  }

  private UserResult findUser(String subject) {
    return findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery(subject)).orElse(null);
  }

  /** The first word is the first name; the rest, the last name. */
  static PersonInfo visitorInfo(String name) {
    var parts = name.split("\\s+", 2);
    return new PersonInfo(parts[0], parts.length > 1 ? parts[1] : "", null, null, null, null, null);
  }
}
