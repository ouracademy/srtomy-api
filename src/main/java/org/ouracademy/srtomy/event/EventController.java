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
import lombok.AllArgsConstructor;
import lombok.Data;

@RestController
@RequestMapping("events")
@AllArgsConstructor
@Tag(name = "events")
public class EventController {

	EventRepository eventRepository;
	SingleEventRepository singleEventRepository;
	PersonRepository personRepository;
	OrganizationRepository organizationRepository;
	EventAttendanceRepository eventAttendanceRepository;

	@GetMapping
	public List<SingleEvent> findAll(
			@RequestParam(required = true) Long organizationId,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
		return eventRepository.searchEventsOfOrgIdAndBetweenDates(
				organizationId,
				start.atZone(ZoneId.of("UTC")),
				end.atZone(ZoneId.of("UTC")));
	}

	@GetMapping("{id}")
	public ResponseEntity<SingleEvent> findById(@PathVariable Long id) {
		return ResponseEntity.of(singleEventRepository.findById(id));
	}

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	@Transactional
	public Long create(@RequestBody @Valid EventRequest eventRequest) {
		var start = eventRequest.startTime.atZone(ZoneId.of("UTC"));
		var end = eventRequest.endTime.atZone(ZoneId.of("UTC"));
		var event = eventRequest.isRecurrent
				? new RecurrentEvent(eventRequest.name, start, end, eventRequest.daysOfWeek)
				: new SingleEvent(eventRequest.name, start, end);
		mapToModel(eventRequest, event);
		var id = eventRepository.save(event).getId();

		if (event instanceof RecurrentEvent recurrentEvent) {
			var events = recurrentEvent.expand();
			eventRepository.saveAll(events);
		}

		return id;
	}

	private void mapToModel(EventRequest eventRequest, Event event) {
		event.organization = organizationRepository.getReferenceById(eventRequest.organizationId);
		event.address = eventRequest.address;
		event.speakers = eventRequest.speakerIds.stream().map(personRepository::getReferenceById).toList();
		event.description = eventRequest.description;
		event.isACourse = eventRequest.isACourse;
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
		EventChangeOption option = EventChangeOption.THIS_EVENT;
	}

	@DeleteMapping("{id}")
	public void delete(@PathVariable Long id, @RequestBody @Valid EventDeleteRequest request) {
		var event = singleEventRepository.findById(id).orElseThrow();
		switch (request.option) {
			case THIS_EVENT:
				eventRepository.delete(event);
				break;
			case THIS_AND_THE_FOLLOWING_EVENTS:
				var followingEvents = singleEventRepository.findByRecurrentEventAndStartAfter(
						event.recurrentEvent,
						event.start);
				eventRepository.deleteAll(followingEvents);
				eventRepository.delete(event);
				break;
			case ALL_EVENTS:
				eventRepository.deleteAll(singleEventRepository.findByRecurrentEvent(event.recurrentEvent));
				eventRepository.delete(event.recurrentEvent);
				break;
		}
	}

	@PutMapping("{id}")
	public ResponseEntity<Event> edit(
			@PathVariable Long id,
			@RequestBody @Valid EditEventRequest request) {

		if (request.isRecurrent) {
			// THIS_AND_THE_FOLLOWING_EVENTS
			// ALL_EVENTS
		} else {
			var event = singleEventRepository.findById(id).orElseThrow();
			var start = request.startTime.atZone(ZoneId.of("UTC"));
			var end = request.endTime.atZone(ZoneId.of("UTC"));

			switch (request.option) {
				case THIS_EVENT:
					event.start = start;
					event.end = end;
					mapToModel(request, event);
					eventRepository.save(event);
					break;
				case THIS_AND_THE_FOLLOWING_EVENTS:
					var followingEvents = singleEventRepository.findByRecurrentEventAndStartAfter(
							event.recurrentEvent,
							event.start);
					
					eventRepository.deleteAll(followingEvents);
					break;
				case ALL_EVENTS:
					
					eventRepository.deleteAll(singleEventRepository.findByRecurrentEvent(event.recurrentEvent));
					break;
			}
		}

		return null;
		// return eventRepository.findById(id).map(organization -> {
		// organization.name = eventRequest.name;
		// organization.phoneNumber = eventRequest.phoneNumber;
		// organization.address = eventRequest.address;
		// organization.parent = eventRequest.parentId == null ? null
		// : eventRepository.getReferenceById(eventRequest.parentId);

		// return ResponseEntity.ok(eventRepository.save(organization));
		// }).orElse(ResponseEntity.notFound().build());
	}

}
