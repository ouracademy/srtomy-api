package org.ouracademy.srtomy.person;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<Person> findAll() {
        // TODO: list organizations of a user
        return personRepository.findAll();
    }

    @GetMapping("{id}")
    public ResponseEntity<Person> findById(@RequestParam Long id) {
        return ResponseEntity.of(personRepository.findById(id));
    }

    @PostMapping
    public Person create(@RequestBody @Valid Person person) {
        return personRepository.save(person);
    }

    @PutMapping("{id}")
    public Person edit(@RequestParam Long id, @RequestBody @Valid Person person) {
        person.id = id;
        return personRepository.save(person);
    }

}
