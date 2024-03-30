package org.ouracademy.srtomy.organization;

import java.util.List;

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

    @GetMapping
    public List<Organization> findAll() {
        // TODO: list organizations of a user
        return organizationRepository.findAll();
    }

    @GetMapping("{id}")
    public ResponseEntity<Organization> findById(@PathVariable Long id) {
        return ResponseEntity.of(organizationRepository.findById(id));
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public Organization create(@RequestBody @Valid OrganizationRequest organizationRequest) {
        var organization = new Organization(organizationRequest.name);
        organization.phoneNumber = organizationRequest.phoneNumber;
        organization.address = organizationRequest.address;
        organization.parent = organizationRequest.parentId == null ? null
                : organizationRepository.getReferenceById(organizationRequest.parentId);

        return organizationRepository.save(organization);
    }

    @PutMapping("{id}")
    public ResponseEntity<Organization> edit(@PathVariable Long id,
            @RequestBody @Valid OrganizationRequest organizationRequest) {
        return organizationRepository.findById(id).map(organization -> {
            organization.name = organizationRequest.name;
            organization.phoneNumber = organizationRequest.phoneNumber;
            organization.address = organizationRequest.address;
            organization.parent = organizationRequest.parentId == null ? null
                    : organizationRepository.getReferenceById(organizationRequest.parentId);

            return ResponseEntity.ok(organizationRepository.save(organization));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("{id}/member-state")
    public List<Accountability> getMemberState(@PathVariable Long id) {
        return accountabilityRepository.findByOrganizationIdAndTypeIn(id, Accountability.MEMBER_ACCOUNTABILITIES);
    }

}
