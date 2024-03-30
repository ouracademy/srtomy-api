package org.ouracademy.srtomy.person;

import org.ouracademy.srtomy.organization.OrganizationRepository;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("people")
@AllArgsConstructor
@Tag(name = "people")
public class PersonController {

    PersonRepository personRepository;
    OrganizationRepository organizationRepository;

    @GetMapping
    public Page<Person> findAll(String searchText, @ParameterObject Pageable pageable) {
        // TODO: list person of organization
        if (searchText != null) {
            return personRepository.findByName(searchText, pageable);
        }

        return personRepository.findAll(pageable);
    }

    @GetMapping("{id}")
    public ResponseEntity<Person> findById(@PathVariable Long id) {
        return ResponseEntity.of(personRepository.findById(id));
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public Long create(@RequestBody @Valid CreatePersonRequest personRequest) {
        var person = toModel(personRequest, new Person());
        person.organization = organizationRepository.getReferenceById(personRequest.organizationId);
        person = personRepository.save(person);
        return person.getId();
    }

    @PutMapping("{id}")
    public ResponseEntity<Long> edit(@PathVariable Long id, @RequestBody @Valid UpdatePersonRequest personRequest) {
        return personRepository.findById(id).map(person -> {
            toModel(personRequest, person);
            return ResponseEntity.ok(personRepository.save(person).getId());
        }).orElse(ResponseEntity.notFound().build());
    }

    private Person toModel(UpdatePersonRequest personRequest, Person person) {
        person.firstName = personRequest.firstName;
        person.lastName = personRequest.lastName;
        person.phoneNumber = personRequest.phoneNumber;
        person.age = personRequest.age;
        person.birthdate = personRequest.birthdate;
        person.hasBeenBaptized = personRequest.hasBeenBaptized;
        return person;
    }

}
