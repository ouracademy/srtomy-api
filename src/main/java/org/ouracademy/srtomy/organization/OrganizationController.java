package org.ouracademy.srtomy.organization;

import java.util.List;

import org.ouracademy.srtomy.organization.Organization.Type;
import org.ouracademy.srtomy.person.Accountability;
import org.ouracademy.srtomy.person.AccountabilityRepository;
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
@RequestMapping("organizations")
@AllArgsConstructor
@Tag(name = "organizations")
public class OrganizationController {

    OrganizationRepository organizationRepository;
    AccountabilityRepository accountabilityRepository;

    @GetMapping("{id}/childs")
    public List<Organization> findAll() {
        // TODO: list organizations of a user
        // TODO: add authorization
        return organizationRepository.findAll();
    }

    @GetMapping("{id}")
    public ResponseEntity<Organization> findById(@PathVariable Long id) {
        return ResponseEntity.of(organizationRepository.findById(id));
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public Organization create(@RequestBody @Valid OrganizationRequest request) {
        return createOrg(request, Type.CHURCH);
    }

    @PostMapping("/locations")
    @ResponseStatus(code = HttpStatus.CREATED)
    public Organization createLocation(@RequestBody @Valid LocationRequest request) {
        return createOrg(request, Type.LOCATION);
    }

    @PostMapping("/locations/childs")
    @ResponseStatus(code = HttpStatus.CREATED)
    public Organization createLocationChild(@RequestBody @Valid LocationChildRequest request) {
        return createOrg(request, request.type);
    }

    private Organization createOrg(OrganizationRequest organizationRequest, Type type) {
        var organization = new Organization(organizationRequest.name);
        fillInfo(organizationRequest, organization);
        organization.type = type;
        // organization.parent = organizationRequest.parentId == null ? null
        // : organizationRepository.getReferenceById(organizationRequest.parentId);

        return organizationRepository.save(organization);
    }

    @PutMapping("{id}")
    public ResponseEntity<Organization> edit(@PathVariable Long id,
            @RequestBody @Valid OrganizationRequest organizationRequest) {
        return organizationRepository.findById(id).map(organization -> {
            organization.setName(organizationRequest.name);
            fillInfo(organizationRequest, organization);

            return ResponseEntity.ok(organizationRepository.save(organization));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void fillInfo(OrganizationRequest organizationRequest, Organization organization) {
        organization.setDescription(organizationRequest.description);
        organization.setPhoneNumber(organizationRequest.phoneNumber);
        organization.setAddress(organizationRequest.address);
    }

    @GetMapping("{id}/member-state")
    public List<Accountability> getMemberState(@PathVariable Long id) {
        return accountabilityRepository.findByParentIdAndTypeIn(id, Accountability.MEMBER_ACCOUNTABILITIES);
    }

}
