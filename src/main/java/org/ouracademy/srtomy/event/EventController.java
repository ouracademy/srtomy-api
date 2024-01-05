package org.ouracademy.srtomy.event;

import java.util.List;

import org.ouracademy.srtomy.organization.OrganizationRepository;
import org.ouracademy.srtomy.person.PersonRepository;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("events")
@AllArgsConstructor
@Tag(name = "events")
public class EventController {

    EventRepository eventRepository;
    PersonRepository personRepository;
    OrganizationRepository organizationRepository;
    AttendanceRepository attendanceRepository;

    @GetMapping
    public Page<EventSimpleItem> findAll(@ParameterObject Pageable pageable) {
        // TODO: list events of an org
        return eventRepository.findSimpleBy(pageable);
    }

    @GetMapping("{id}")
    public ResponseEntity<Event> findById(@PathVariable Long id) {
        return ResponseEntity.of(eventRepository.findById(id));
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public Event create(@RequestBody @Valid EventRequest eventRequest) {
        var event = new Event(eventRequest.name, eventRequest.startTime, eventRequest.endTime);
        event.organization = organizationRepository.getReferenceById(eventRequest.organizationId);
        event.address = eventRequest.address;
        event.speakers = eventRequest.speakerIds.stream().map(personRepository::getReferenceById).toList();
        return eventRepository.save(event);
    }

    @GetMapping("{id}/attendance")
    public List<Long> getAttendance(@PathVariable Long id) {
        var personIds = attendanceRepository.findPersonIdsByEventId(id);
        return personIds;
    }

    @PutMapping("{id}/attendance")
    @ResponseStatus(code = HttpStatus.CREATED)
    @Transactional
    public void registerAttendance(@PathVariable Long id, @RequestBody @Valid AttendanceRequest attendanceRequest) {
        var event = eventRepository.getReferenceById(id);
        var attendances = attendanceRequest.personIds.stream()
                .map(personId -> new Attendance(event, personRepository.getReferenceById(personId)))
                .toList();
        attendanceRepository.deleteAllByEvent(event);
        attendanceRepository.saveAll(attendances);
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
