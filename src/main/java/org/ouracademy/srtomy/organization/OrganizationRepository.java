package org.ouracademy.srtomy.organization;

import org.ouracademy.srtomy.person.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, PartyId> {}
