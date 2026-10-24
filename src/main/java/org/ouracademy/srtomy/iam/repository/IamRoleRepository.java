package org.ouracademy.srtomy.iam.repository;

import java.util.List;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IamRoleRepository extends JpaRepository<IamRole, IamRoleId> {

  /** The predefined roles first, then the church's own, both by name. */
  @Query(
      """
      select r from IamRole r
      where r.church is null or r.church.id = :churchId
      order by case when r.church is null then 0 else 1 end, r.name
      """)
  List<IamRole> findAvailableTo(PartyId churchId);

  /** True when a predefined role or a live role of the church already uses the name. */
  @Query(
      """
      select count(r) > 0 from IamRole r
      where lower(r.name) = lower(:name)
      and (r.church is null or r.church.id = :churchId)
      and r.id <> :excludedId
      """)
  boolean nameTaken(PartyId churchId, String name, IamRoleId excludedId);
}
