package org.ouracademy.srtomy.person;

import java.util.List;

import org.ouracademy.srtomy.person.Accountability.Type;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityRepository extends JpaRepository<Accountability, Long> {

    List<Accountability> findByOrganizationIdAndTypeIn(Long id, List<Type> types);
}
