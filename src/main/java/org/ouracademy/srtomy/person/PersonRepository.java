package org.ouracademy.srtomy.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonRepository extends JpaRepository<Person, Long> {

    @Query("""
            select p from Person p
            where
            lower(p.firstName) like lower(CONCAT('%', :searchText, '%'))
            or lower(p.lastName) like lower(CONCAT('%', :searchText, '%'))
            """)
    Page<Person> findByName(String searchText, Pageable pageable);
}
