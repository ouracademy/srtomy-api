package org.ouracademy.srtomy.organization;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("organizations")
@AllArgsConstructor
public class OrganizationController {

    OrganizationRepository organizationRepository;

    @GetMapping
    public List<Organization> findAll() {
        // TODO: list organizations of a user
        return organizationRepository.findAll();
    }

    @GetMapping("{id}")
    public ResponseEntity<Organization> findById(@RequestParam Long id) {
        return ResponseEntity.of(organizationRepository.findById(id));
    }

    @PostMapping
    public Organization create(@RequestBody @Valid Organization organization) {
        return organizationRepository.save(organization);
    }

    @PutMapping("{id}")
    public Organization edit(@RequestParam Long id, @RequestBody @Valid Organization organization) {
        organization.id = id;
        return organizationRepository.save(organization);
    }

}
