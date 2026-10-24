package org.ouracademy.srtomy.iam.repository;

import java.util.List;
import java.util.UUID;
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

  /** The predefined roles, by name. */
  @Query("select r from IamRole r where r.church is null order by r.name")
  List<IamRole> findPredefined();

  /** How many live positions (cargos) use the role as their access level. */
  @Query(
      value =
          "select count(*) from accountability_type where iam_role_id = :roleId"
              + " and deleted_time is null",
      nativeQuery = true)
  long countPositionsUsing(UUID roleId);

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
