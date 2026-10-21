package org.ouracademy.srtomy.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonRepository extends JpaRepository<Person, PartyId> {

  @Query(
      """
            select p
            from Accountability a join TREAT(a.child as Person) p
            where a.type = :accountabilityType
            and a.parent.id = :organizationId
            and (
                lower(p.firstName) like lower(CONCAT('%', :searchText, '%'))
                or lower(p.lastName) like lower(CONCAT('%', :searchText, '%'))
            )
            """)
  Page<Person> findByNameAndOrgId(
      String searchText,
      PartyId organizationId,
      AccountabilityType accountabilityType,
      Pageable pageable);

  @Query(
      """
            select p
            from Accountability a join TREAT(a.child as Person) p
            where a.type = :accountabilityType
            and a.parent.id = :organizationId
            """)
  Page<Person> findByOrgId(
      PartyId organizationId, AccountabilityType accountabilityType, Pageable pageable);
}
