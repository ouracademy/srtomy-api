package org.ouracademy.srtomy.person;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.accountability.command.SyncPersonRolesCommand;
import org.ouracademy.srtomy.accountability.command.SyncPersonRolesCommandHandler;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("people")
@AllArgsConstructor
@Tag(name = "people")
public class PersonController {

  AccountabilityRepository accountabilityRepository;
  AccountabilityTypeRepository accountabilityTypeRepository;
  PersonRepository personRepository;
  OrganizationRepository organizationRepository;
  SyncPersonRolesCommandHandler syncPersonRolesCommandHandler;

  @GetMapping
  public Page<Person> findAll(
      @RequestParam(required = true) UUID organizationId,
      String searchText,
      @ParameterObject Pageable pageable) {

    var isPartOf =
        accountabilityTypeRepository.getReferenceById(SystemAccountabilityTypes.IS_PART_OF);
    var organizationPartyId = new PartyId(organizationId);

    if (searchText != null) {
      return personRepository.findByNameAndOrgId(
          searchText, organizationPartyId, isPartOf, pageable);
    }

    return personRepository.findByOrgId(organizationPartyId, isPartOf, pageable);
  }

  @GetMapping("{id}")
  public ResponseEntity<Person> findById(@PathVariable UUID id) {
    return ResponseEntity.of(personRepository.findById(new PartyId(id)));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  @Transactional
  public UUID create(@RequestBody @Valid CreatePersonRequest personRequest) {
    var person = toModel(personRequest, new Person());
    person = personRepository.save(person);

    var organizationId = new PartyId(personRequest.organizationId);
    var organization = organizationRepository.getReferenceById(organizationId);

    syncPersonRolesCommandHandler.handle(
        new SyncPersonRolesCommand(
            person.getId(), organizationId, toAccountabilityTypeIds(personRequest.roles)));

    var visitorState =
        accountabilityTypeRepository.getReferenceById(SystemAccountabilityTypes.VISITOR);
    accountabilityRepository.save(new Accountability(person, organization, visitorState));

    var isPartOf =
        accountabilityTypeRepository.getReferenceById(SystemAccountabilityTypes.IS_PART_OF);
    accountabilityRepository.save(new Accountability(person, organization, isPartOf));

    return person.getId().value();
  }

  @PutMapping("{id}")
  @Transactional
  public ResponseEntity<UUID> edit(
      @PathVariable UUID id, @RequestBody @Valid UpdatePersonRequest personRequest) {
    return personRepository
        .findById(new PartyId(id))
        .map(
            person -> {
              toModel(personRequest, person);
              syncPersonRolesCommandHandler.handle(
                  new SyncPersonRolesCommand(
                      person.getId(),
                      new PartyId(personRequest.organizationId),
                      toAccountabilityTypeIds(personRequest.roles)));
              return ResponseEntity.ok(personRepository.save(person).getId().value());
            })
        .orElse(ResponseEntity.notFound().build());
  }

  private Set<AccountabilityTypeId> toAccountabilityTypeIds(Set<UUID> ids) {
    return ids.stream().map(AccountabilityTypeId::new).collect(Collectors.toSet());
  }

  private Person toModel(CreatePersonRequest personRequest, Person person) {
    person.firstName = personRequest.firstName;
    person.lastName = personRequest.lastName;
    person.setName(personRequest.firstName + " " + personRequest.lastName);
    person.setPhoneNumber(personRequest.phoneNumber);
    person.age = personRequest.age;
    person.birthdate = personRequest.birthdate;
    person.hasBeenBaptized = personRequest.hasBeenBaptized;
    return person;
  }
}
