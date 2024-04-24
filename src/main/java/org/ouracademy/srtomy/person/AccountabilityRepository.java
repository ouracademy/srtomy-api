package org.ouracademy.srtomy.person;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityRepository extends JpaRepository<Accountability, Long> {

    List<Accountability> findByParentIdAndTypeIn(Long id, List<Long> types);

    void deleteAllByType(AccountabilityType roleToDelete);
}
