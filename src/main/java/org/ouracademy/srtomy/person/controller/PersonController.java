package org.ouracademy.srtomy.person.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQuery;
import org.ouracademy.srtomy.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
  FindOrganizationsOfMemberQueryHandler findOrganizationsOfMemberQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  @GetMapping
  public Page<PersonResponse> findAll(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = true) UUID organizationId,
      String searchText,
      @ParameterObject Pageable pageable) {
    authorize(jwt, new PartyId(organizationId), Permission.MEMBERS_READ);
    return searchPeopleQueryHandler
        .handle(new SearchPeopleQuery(new PartyId(organizationId), searchText, pageable))
        .people()
        .map(PersonResponse::from);
  }

  @GetMapping("{id}")
  public ResponseEntity<PersonResponse> findById(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    // A person may belong to several nodes; reading it needs the permission on any of them.
    authorizeQueryHandler.handle(
        new AuthorizeQuery(jwt.getSubject(), organizationsOf(id), Permission.MEMBERS_READ));
    return ResponseEntity.of(
        findPersonByIdQueryHandler
            .handle(new FindPersonByIdQuery(new PartyId(id)))
            .map(PersonResponse::from));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public UUID create(
      @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreatePersonRequest request) {
    var organizationId = new PartyId(request.getOrganizationId());
    authorize(jwt, organizationId, Permission.MEMBERS_WRITE);
    return createPersonCommandHandler
        .handle(
            new CreatePersonCommand(
                jwt.getSubject(),
                info(request),
                organizationId,
                positionIds(request.getPositionIds())))
        .id()
        .value();
  }

  @PutMapping("{id}")
  public ResponseEntity<UUID> edit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid UpdatePersonRequest request) {
    var organizationId = new PartyId(request.getOrganizationId());
    authorize(jwt, organizationId, Permission.MEMBERS_WRITE);
    // Otherwise a writer of one node could edit anyone by naming their own node.
    if (!organizationsOf(id).contains(organizationId)) {
      throw new AccessDeniedException("La persona no pertenece a este grupo");
    }
    return ResponseEntity.of(
        updatePersonCommandHandler
            .handle(
                new UpdatePersonCommand(
                    jwt.getSubject(),
                    new PartyId(id),
                    info(request),
                    organizationId,
                    positionIds(request.getPositionIds())))
            .map(result -> result.id().value()));
  }

  private static PersonInfo info(CreatePersonRequest request) {
    return new PersonInfo(
        request.getFirstName(),
        request.getLastName(),
        request.getPhoneNumber(),
        request.getEmail(),
        request.getAge(),
        request.getBirthdate(),
        request.getHasBeenBaptized());
  }

  private void authorize(Jwt jwt, PartyId organizationId, Permission permission) {
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), organizationId, permission));
  }

  private List<PartyId> organizationsOf(UUID personId) {
    return findOrganizationsOfMemberQueryHandler
        .handle(new FindOrganizationsOfMemberQuery(new PartyId(personId)))
        .organizationIds();
  }

  private static Set<AccountabilityTypeId> positionIds(Set<UUID> ids) {
    return ids.stream().map(AccountabilityTypeId::new).collect(Collectors.toSet());
  }
}
