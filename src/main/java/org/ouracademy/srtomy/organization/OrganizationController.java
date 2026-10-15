package org.ouracademy.srtomy.organization;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.ouracademy.srtomy.person.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.person.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.srtomy.person.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ouracademy.srtomy.auth0.Auth0Gateway;
import org.ouracademy.srtomy.organization.Organization.Type;
import org.ouracademy.srtomy.person.Accountability;
import org.ouracademy.srtomy.person.AccountabilityRepository;
import org.ouracademy.srtomy.person.AccountabilityType;
import org.ouracademy.srtomy.person.AccountabilityTypeRepository;
import org.ouracademy.srtomy.person.Party;
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
  RoleService roleService;
  AccountabilityTypeRepository accountabilityTypeRepository;
  Auth0Gateway auth0Gateway;

  @GetMapping("{id}/childs")
  public List<Party> findAll(@PathVariable Long id) {
    // TODO: list organizations of a user
    // TODO: add authorization

    return accountabilityRepository.findByParentIdAndTypeIn(id, types(IS_PART_OF)).stream()
        .map(x -> x.getChild())
        .filter(x -> x instanceof Organization)
        .toList();
  }

  @GetMapping("{id}")
  public ResponseEntity<Organization> findById(@PathVariable Long id) {
    return ResponseEntity.of(organizationRepository.findById(id));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  @Transactional
  // TODO: use saga to update user metadata (to make the db transaction more short)
  public Organization create(@RequestBody @Valid OrganizationRequest request) {
    var org = createOrg(request, Type.CHURCH);
    var accessToken = auth0Gateway.getAccessToken();
    var userMetadata = Map.of("orgId", org.getId());
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
    roleService.sync(location, request.roles);
    return location;
  }

  @PutMapping("/locations/{id}")
  @Transactional
  public ResponseEntity<Organization> updateLocation(
      @PathVariable Long id, @RequestBody @Valid UpdateLocationRequest request) {
    return editOrgInfo(id, request)
        .map(
            org -> {
              roleService.sync(org, request.roles);
              return ResponseEntity.ok(org);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("locations/predefined-roles")
  public List<AccountabilityTypeDto> getPredefinedRoles() {
    return roleService.getPredefinedRoles().stream()
        .map(x -> new AccountabilityTypeDto(x.getId(), x.getName(), x.isPredefined()))
        .toList();
  }

  @GetMapping("locations/{id}/roles")
  public List<AccountabilityTypeDto> getRolesOfLocation(@PathVariable Long id) {
    return roleService
        .getRolesOfLocation(id)
        .map(x -> new AccountabilityTypeDto(x.getId(), x.getName(), x.isPredefined()))
        .toList();
  }

  @GetMapping("locations/{id}/person/{personId}/roles")
  public List<AccountabilityTypeDto> getRolesOfPerson(
      @PathVariable Long id, @PathVariable Long personId) {
    return roleService
        .getRolesOfPerson(id, personId)
        .map(x -> new AccountabilityTypeDto(x.getId(), x.getName(), x.isPredefined()))
        .toList();
  }

  @Getter
  @AllArgsConstructor
  public static class AccountabilityTypeDto {
    Long id;
    String name;
    Boolean isPredefined;
  }

  @GetMapping("/locations/childs/{id}/members")
  public Map<String, List<Party>> getLocationChilds(@PathVariable Long id) {
    return accountabilityRepository.findByParentIdAndTypeIn(id, types(IS_PART_OF, LEADER)).stream()
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
                                personRepository.getReferenceById(id),
                                locationChild,
                                accountabilityTypeRepository.getReferenceById(IS_PART_OF))),
                request.leaderIds.stream()
                    .map(
                        id ->
                            new Accountability(
                                personRepository.getReferenceById(id),
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
      var parent = organizationRepository.getReferenceById(organizationRequest.parentId);
      var accountabilityType = accountabilityTypeRepository.getReferenceById(IS_PART_OF);

      accountabilityRepository.save(new Accountability(organization, parent, accountabilityType));
    }

    return organization;
  }

  @PutMapping("{id}")
  public ResponseEntity<Organization> edit(
      @PathVariable Long id, @RequestBody @Valid OrganizationRequest organizationRequest) {
    return editOrgInfo(id, organizationRequest)
        .map(org -> ResponseEntity.ok(org))
        .orElse(ResponseEntity.notFound().build());
  }

  private Optional<Organization> editOrgInfo(Long id, OrganizationRequest organizationRequest) {
    return organizationRepository
        .findById(id)
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
  public List<Accountability> getMemberState(@PathVariable Long id) {
    return accountabilityRepository.findByParentIdAndTypeIn(id, types(MEMBER_PROGRESS_STATES));
  }

  private List<AccountabilityType> types(Long... ids) {
    return List.of(ids).stream()
        .map(x -> accountabilityTypeRepository.getReferenceById(x))
        .toList();
  }

  @DeleteMapping("{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    return organizationRepository
        .findById(id)
        .map(
            organization -> {
              if (organization.getType() != Type.GROUP && organization.getType() != Type.MINISTRY) {
                return ResponseEntity.badRequest().build();
              }

              accountabilityRepository.deleteAllByParentId(id);
              accountabilityRepository.deleteAllByChildId(id);
              organizationRepository.deleteById(id);

              return ResponseEntity.noContent().build();
            })
        .orElse(ResponseEntity.notFound().build());
  }
}
