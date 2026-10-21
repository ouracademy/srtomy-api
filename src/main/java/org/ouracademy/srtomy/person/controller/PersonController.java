package org.ouracademy.srtomy.person.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.PersonInfo;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonCommand;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.srtomy.person.command.update_person.UpdatePersonCommand;
import org.ouracademy.srtomy.person.command.update_person.UpdatePersonCommandHandler;
import org.ouracademy.srtomy.person.controller.dto.CreatePersonRequest;
import org.ouracademy.srtomy.person.controller.dto.PersonResponse;
import org.ouracademy.srtomy.person.controller.dto.UpdatePersonRequest;
import org.ouracademy.srtomy.person.query.find_person_by_id.FindPersonByIdQuery;
import org.ouracademy.srtomy.person.query.find_person_by_id.FindPersonByIdQueryHandler;
import org.ouracademy.srtomy.person.query.search_people.SearchPeopleQuery;
import org.ouracademy.srtomy.person.query.search_people.SearchPeopleQueryHandler;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("people")
@AllArgsConstructor
@Tag(name = "people")
public class PersonController {

  SearchPeopleQueryHandler searchPeopleQueryHandler;
  FindPersonByIdQueryHandler findPersonByIdQueryHandler;
  CreatePersonCommandHandler createPersonCommandHandler;
  UpdatePersonCommandHandler updatePersonCommandHandler;

  @GetMapping
  public Page<PersonResponse> findAll(
      @RequestParam(required = true) UUID organizationId,
      String searchText,
      @ParameterObject Pageable pageable) {
    return searchPeopleQueryHandler
        .handle(new SearchPeopleQuery(new PartyId(organizationId), searchText, pageable))
        .people()
        .map(PersonResponse::from);
  }

  @GetMapping("{id}")
  public ResponseEntity<PersonResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.of(
        findPersonByIdQueryHandler
            .handle(new FindPersonByIdQuery(new PartyId(id)))
            .map(PersonResponse::from));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public UUID create(@RequestBody @Valid CreatePersonRequest request) {
    return createPersonCommandHandler
        .handle(
            new CreatePersonCommand(
                info(request), new PartyId(request.getOrganizationId()), roles(request.getRoles())))
        .id()
        .value();
  }

  @PutMapping("{id}")
  public ResponseEntity<UUID> edit(
      @PathVariable UUID id, @RequestBody @Valid UpdatePersonRequest request) {
    return ResponseEntity.of(
        updatePersonCommandHandler
            .handle(
                new UpdatePersonCommand(
                    new PartyId(id),
                    info(request),
                    new PartyId(request.getOrganizationId()),
                    roles(request.getRoles())))
            .map(result -> result.id().value()));
  }

  private static PersonInfo info(CreatePersonRequest request) {
    return new PersonInfo(
        request.getFirstName(),
        request.getLastName(),
        request.getPhoneNumber(),
        request.getAge(),
        request.getBirthdate(),
        request.getHasBeenBaptized());
  }

  private static Set<AccountabilityTypeId> roles(Set<UUID> ids) {
    return ids.stream().map(AccountabilityTypeId::new).collect(Collectors.toSet());
  }
}
