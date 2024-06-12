package org.ouracademy.srtomy.event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.ouracademy.srtomy.organization.OrganizationRepository;
import org.ouracademy.srtomy.person.PersonRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@RestController
@RequestMapping("events")
@AllArgsConstructor
@Tag(name = "events")
public class EventController {

	EventRepository eventRepository;
	PersonRepository personRepository;
	OrganizationRepository organizationRepository;
	EventAttendanceRepository eventAttendanceRepository;

	@GetMapping
	public List<Event> findAll(
			@RequestParam(required = true) Long organizationId,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
		return eventRepository.searchEventsOfOrgIdAndBetweenDates(
				organizationId,
				start.atZone(ZoneId.of("UTC")),
				end.atZone(ZoneId.of("UTC")));
	}

	@GetMapping("{id}")
	public ResponseEntity<Event> findById(@PathVariable Long id) {
		return ResponseEntity.of(eventRepository.findById(id));
	}

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public Long create(@RequestBody @Valid EventRequest eventRequest) {
		var start = eventRequest.startTime.atZone(ZoneId.of("UTC"));
		var end = eventRequest.endTime.atZone(ZoneId.of("UTC"));
		var event = eventRequest.isRecurrent
				? new RecurrentEvent(eventRequest.name, start, end, eventRequest.daysOfWeek)
				: new SingleEvent(eventRequest.name, start, end);
		event.organization = organizationRepository.getReferenceById(eventRequest.organizationId);
		event.address = eventRequest.address;
		event.speakers = eventRequest.speakerIds.stream().map(personRepository::getReferenceById).toList();
		event.description = eventRequest.description;
		event.isACourse = eventRequest.isACourse;
		return eventRepository.save(event).getId();
	}

	@GetMapping("{id}/attendance")
	public ResponseEntity<EventAttendanceResponse> getAttendance(@PathVariable Long id) {
		return eventAttendanceRepository
				.findByEventId(id)
				.map(x -> EventAttendanceResponse.builder()
						.personIds(x.getPersonIds())
						.build())
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.noContent().build());
	}

	@PutMapping("{id}/attendance")
	@ResponseStatus(code = HttpStatus.CREATED)
	@Transactional
	public void registerAttendance(@PathVariable Long id, @RequestBody @Valid AttendanceRequest attendanceRequest) {
		var eventAttendance = eventAttendanceRepository.findByEventId(id).orElse(
				new EventAttendance(eventRepository.getReferenceById(id)));

		eventAttendance = eventAttendanceRepository.save(eventAttendance);
		eventAttendance.setPeople(
				attendanceRequest.personIds.stream()
						.map(personId -> personRepository.getReferenceById(personId))
						.toList());
		eventAttendanceRepository.save(eventAttendance);
	}

	@Data
	public static class EventDeleteRequest {
		@NotNull
		LocalDateTime startTime;
		@NotNull
		LocalDateTime endTime;
		Option option;

		enum Option {
			THIS_EVENT,
			THIS_AND_THE_FOLLOWING_EVENTS,
			ALL_EVENTS
		}
	}

	@DeleteMapping("{id}")
	public void delete(@PathVariable Long id, @RequestBody @Valid EventDeleteRequest request) {
		var event = eventRepository
				.findById(id)
				.orElseThrow();
		if (event instanceof RecurrentEvent) {
			switch (request.option) {
				case THIS_EVENT:

					break;
				case THIS_AND_THE_FOLLOWING_EVENTS:

					break;
				case ALL_EVENTS:

					break;
			}
		} else {
			eventRepository.delete(event);
		}
	}

	// @PutMapping("{id}")
	// public ResponseEntity<Event> edit(
	// @PathVariable Long id,
	// @RequestBody @Valid EventRequest eventRequest) {

	// // return eventRepository.findById(id).map(organization -> {
	// // organization.name = eventRequest.name;
	// // organization.phoneNumber = eventRequest.phoneNumber;
	// // organization.address = eventRequest.address;
	// // organization.parent = eventRequest.parentId == null ? null
	// // : eventRepository.getReferenceById(eventRequest.parentId);

	// // return ResponseEntity.ok(eventRepository.save(organization));
	// // }).orElse(ResponseEntity.notFound().build());
	// }

}
