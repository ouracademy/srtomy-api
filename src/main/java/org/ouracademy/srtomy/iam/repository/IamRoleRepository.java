package org.ouracademy.srtomy.iam.repository;

import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamRoleRepository extends JpaRepository<IamRole, IamRoleId> {}
