package org.ouracademy.srtomy.organization;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ouracademy.srtomy.accountability.command.SyncOrganizationRolesCommand;
import org.ouracademy.srtomy.accountability.command.SyncOrganizationRolesCommandHandler;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.query.GetPredefinedRolesQuery;
import org.ouracademy.srtomy.accountability.query.GetPredefinedRolesQueryHandler;
import org.ouracademy.srtomy.accountability.query.GetRolesOfLocationQuery;
import org.ouracademy.srtomy.accountability.query.GetRolesOfLocationQueryHandler;
import org.ouracademy.srtomy.accountability.query.GetRolesOfPersonQuery;
import org.ouracademy.srtomy.accountability.query.GetRolesOfPersonQueryHandler;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.auth.gateway.Auth0Gateway;
import org.ouracademy.srtomy.organization.Organization.Type;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.PersonRepository;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("organizations")
@AllArgsConstructor
@Tag(name = "organizations")
public class OrganizationController {

  OrganizationRepository organizationRepository;
  AccountabilityRepository accountabilityRepository;
  PersonRepository personRepository;
  SyncOrganizationRolesCommandHandler syncOrganizationRolesCommandHandler;
  GetPredefinedRolesQueryHandler getPredefinedRolesQueryHandler;
  GetRolesOfLocationQueryHandler getRolesOfLocationQueryHandler;
  GetRolesOfPersonQueryHandler getRolesOfPersonQueryHandler;
  AccountabilityTypeRepository accountabilityTypeRepository;
  Auth0Gateway auth0Gateway;

  @GetMapping("{id}/childs")
  public List<Party> findAll(@PathVariable UUID id) {
    // TODO: list organizations of a user
    // TODO: add authorization

    return accountabilityRepository
        .findByParentIdAndTypeIn(new PartyId(id), types(IS_PART_OF))
        .stream()
        .map(x -> x.getChild())
        .filter(x -> x instanceof Organization)
        .toList();
  }

  @GetMapping("{id}")
  public ResponseEntity<Organization> findById(@PathVariable UUID id) {
    return ResponseEntity.of(organizationRepository.findById(new PartyId(id)));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  @Transactional
  // TODO: use saga to update user metadata (to make the db transaction more short)
  public Organization create(@RequestBody @Valid OrganizationRequest request) {
    var org = createOrg(request, Type.CHURCH);
    var accessToken = auth0Gateway.getAccessToken();
    var userMetadata = Map.of("orgId", org.getId().value());
    var responseUser = auth0Gateway.updateAppMetadata(accessToken, request.userId, userMetadata);
    if (!responseUser.getStatusCode().is2xxSuccessful()) {
      throw new IllegalStateException(
          "Couldn't update user metadata:" + request.getUserId() + "," + userMetadata);
    }
    return org;
  }

  @PostMapping("/locations")
  @ResponseStatus(code = HttpStatus.CREATED)
  @Transactional
  public Organization createLocation(@RequestBody @Valid CreateLocationRequest request) {
    Organization location = createOrg(request, Type.LOCATION);
    syncOrganizationRolesCommandHandler.handle(
        new SyncOrganizationRolesCommand(location.getId(), request.roles));
    return location;
  }

  @PutMapping("/locations/{id}")
  @Transactional
  public ResponseEntity<Organization> updateLocation(
      @PathVariable UUID id, @RequestBody @Valid UpdateLocationRequest request) {
    return editOrgInfo(id, request)
        .map(
            org -> {
              syncOrganizationRolesCommandHandler.handle(
                  new SyncOrganizationRolesCommand(org.getId(), request.roles));
              return ResponseEntity.ok(org);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("locations/predefined-roles")
  public List<AccountabilityTypeDto> getPredefinedRoles() {
    return getPredefinedRolesQueryHandler.handle(new GetPredefinedRolesQuery()).stream()
        .map(AccountabilityTypeDto::from)
        .toList();
  }

  @GetMapping("locations/{id}/roles")
  public List<AccountabilityTypeDto> getRolesOfLocation(@PathVariable UUID id) {
    return getRolesOfLocationQueryHandler
        .handle(new GetRolesOfLocationQuery(new PartyId(id)))
        .stream()
        .map(AccountabilityTypeDto::from)
        .toList();
  }

  @GetMapping("locations/{id}/person/{personId}/roles")
  public List<AccountabilityTypeDto> getRolesOfPerson(
      @PathVariable UUID id, @PathVariable UUID personId) {
    return getRolesOfPersonQueryHandler
        .handle(new GetRolesOfPersonQuery(new PartyId(id), new PartyId(personId)))
        .stream()
        .map(AccountabilityTypeDto::from)
        .toList();
  }

  @Getter
  @AllArgsConstructor
  public static class AccountabilityTypeDto {
    UUID id;
    String name;
    Boolean isPredefined;

    static AccountabilityTypeDto from(AccountabilityTypeResult result) {
      return new AccountabilityTypeDto(result.id().value(), result.name(), result.predefined());
    }
  }

  @GetMapping("/locations/childs/{id}/members")
  public Map<String, List<Party>> getLocationChilds(@PathVariable UUID id) {
    return accountabilityRepository
        .findByParentIdAndTypeIn(new PartyId(id), types(IS_PART_OF, LEADER))
        .stream()
        .collect(groupingBy(x -> x.getType().getName(), mapping(x -> x.getChild(), toList())));
  }

  @PostMapping("/locations/childs")
  @ResponseStatus(code = HttpStatus.CREATED)
  @Transactional
  public Organization createLocationChild(@RequestBody @Valid LocationChildRequest request) {
    Organization locationChild = createOrg(request, request.type);
    var accountabilities =
        Stream.concat(
                request.memberIds.stream()
                    .map(
                        id ->
                            new Accountability(
                                personRepository.getReferenceById(new PartyId(id)),
                                locationChild,
                                accountabilityTypeRepository.getReferenceById(IS_PART_OF))),
                request.leaderIds.stream()
                    .map(
                        id ->
                            new Accountability(
                                personRepository.getReferenceById(new PartyId(id)),
                                locationChild,
                                accountabilityTypeRepository.getReferenceById(LEADER))))
            .toList();
    accountabilityRepository.saveAll(accountabilities);
    return locationChild;
  }

  @Transactional
  public Organization createOrg(OrganizationRequest organizationRequest, Type type) {
    var organization = new Organization(organizationRequest.name);
    fillInfo(organizationRequest, organization);
    organization.type = type;
    organization = organizationRepository.save(organization);

    if (organizationRequest.parentId != null) {
      var parent =
          organizationRepository.getReferenceById(new PartyId(organizationRequest.parentId));
      var accountabilityType = accountabilityTypeRepository.getReferenceById(IS_PART_OF);

      accountabilityRepository.save(new Accountability(organization, parent, accountabilityType));
    }

    return organization;
  }

  @PutMapping("{id}")
  public ResponseEntity<Organization> edit(
      @PathVariable UUID id, @RequestBody @Valid OrganizationRequest organizationRequest) {
    return editOrgInfo(id, organizationRequest)
        .map(org -> ResponseEntity.ok(org))
        .orElse(ResponseEntity.notFound().build());
  }

  private Optional<Organization> editOrgInfo(UUID id, OrganizationRequest organizationRequest) {
    return organizationRepository
        .findById(new PartyId(id))
        .map(
            organization -> {
              organization.setName(organizationRequest.name);
              fillInfo(organizationRequest, organization);

              return organizationRepository.save(organization);
            });
  }

  private void fillInfo(OrganizationRequest organizationRequest, Organization organization) {
    organization.setDescription(organizationRequest.description);
    organization.setPhoneNumber(organizationRequest.phoneNumber);
    organization.setAddress(organizationRequest.address);
  }

  @GetMapping("{id}/member-state")
  public List<Accountability> getMemberState(@PathVariable UUID id) {
    return accountabilityRepository.findByParentIdAndTypeIn(
        new PartyId(id), types(MEMBER_PROGRESS_STATES));
  }

  private List<AccountabilityType> types(AccountabilityTypeId... ids) {
    return List.of(ids).stream()
        .map(x -> accountabilityTypeRepository.getReferenceById(x))
        .toList();
  }

  @DeleteMapping("{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    var partyId = new PartyId(id);
    return organizationRepository
        .findById(partyId)
        .map(
            organization -> {
              if (organization.getType() != Type.GROUP && organization.getType() != Type.MINISTRY) {
                return ResponseEntity.badRequest().build();
              }

              accountabilityRepository.deleteAllByParentId(partyId);
              accountabilityRepository.deleteAllByChildId(partyId);
              organizationRepository.deleteById(partyId);

              return ResponseEntity.noContent().build();
            })
        .orElse(ResponseEntity.notFound().build());
  }
}
