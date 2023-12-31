package org.ouracademy.srtomy.person;

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
    public Person create(@RequestBody @Valid Person person) {
        return personRepository.save(person);
    }

    @PutMapping("{id}")
    public ResponseEntity<Person> edit(@PathVariable Long id, @RequestBody @Valid Person personRequest) {
        return personRepository.findById(id).map(person -> {
            person.firstName = personRequest.firstName;
            person.lastName = personRequest.lastName;
            person.phoneNumber = personRequest.phoneNumber;
            person.age = personRequest.age;
            person.birthdate = personRequest.birthdate;

            return ResponseEntity.ok(personRepository.save(person));
        }).orElse(ResponseEntity.notFound().build());
    }

}
