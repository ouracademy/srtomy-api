package org.ouracademy.ovejafiel.person.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQuery;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonCommand;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.restore_person.RestorePersonCommand;
import org.ouracademy.ovejafiel.person.command.restore_person.RestorePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonCommand;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.controller.dto.CreatePersonRequest;
import org.ouracademy.ovejafiel.person.controller.dto.PersonMatchResponse;
import org.ouracademy.ovejafiel.person.controller.dto.PersonResponse;
import org.ouracademy.ovejafiel.person.controller.dto.UpdatePersonRequest;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQuery;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesQuery;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesQueryHandler;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleQuery;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.ConflictException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
  DeletePersonCommandHandler deletePersonCommandHandler;
  RestorePersonCommandHandler restorePersonCommandHandler;
  FindPersonMatchesQueryHandler findPersonMatchesQueryHandler;
  FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;

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
                positionIds(request.getPositionIds()),
                request.getMemberState()))
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
                    positionIds(request.getPositionIds()),
                    request.getMemberState()))
            .map(result -> result.id().value()));
  }

  /**
   * People of the church that look like the one being registered (by name or email), removed ones
   * included, so the form can say "is it this person?" instead of creating a duplicate.
   */
  @GetMapping("matches")
  public List<PersonMatchResponse> findMatches(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID organizationId,
      @RequestParam(required = false) String searchText,
      @RequestParam(required = false) String email) {
    var node = new PartyId(organizationId);
    authorize(jwt, node, Permission.MEMBERS_WRITE);
    return findPersonMatchesQueryHandler
        .handle(new FindPersonMatchesQuery(node, searchText, email))
        .matches()
        .stream()
        .map(PersonMatchResponse::from)
        .toList();
  }

  /**
   * Removes the person from the node (and what hangs from it). From the church itself it removes
   * them from the church altogether; if a user represents them, that answers 409 {@code
   * PERSON_HAS_ACCOUNT} until {@code confirmUnlinkUser} is sent.
   */
  @DeleteMapping("{id}")
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam UUID organizationId,
      @RequestParam(defaultValue = "false") boolean confirmUnlinkUser) {
    var node = new PartyId(organizationId);
    authorize(jwt, node, Permission.MEMBERS_DELETE);
    // Otherwise a deleter of one node could remove anyone by naming their own node.
    var subtree =
        findOrganizationSubtreeQueryHandler.handle(new FindOrganizationSubtreeQuery(node)).ids();
    if (organizationsOf(id).stream().noneMatch(subtree::contains)) {
      throw new AccessDeniedException("La persona no pertenece a este grupo");
    }
    var result =
        deletePersonCommandHandler.handle(
            new DeletePersonCommand(new PartyId(id), node, confirmUnlinkUser));
    switch (result.outcome()) {
      case NOT_FOUND -> throw new NoSuchElementException("Person not found: " + id);
      case REQUIRES_USER_UNLINK ->
          throw new ConflictException(
              "PERSON_HAS_ACCOUNT",
              "La persona tiene una cuenta. Si la eliminas, perderá el acceso a esta iglesia.");
      case DELETED, REMOVED_FROM_NODE -> {
        // done
      }
    }
  }

  /** Brings back a removed person as a visitor of the node; a new membership, history is kept. */
  @PostMapping("{id}/restore")
  public UUID restore(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestParam UUID organizationId) {
    var node = new PartyId(organizationId);
    authorize(jwt, node, Permission.MEMBERS_WRITE);
    return restorePersonCommandHandler
        .handle(new RestorePersonCommand(new PartyId(id), node))
        .id()
        .value();
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
