package org.ouracademy.srtomy.iam.query.expand_permissions;

import java.util.Collection;
import org.ouracademy.srtomy.iam.domain.IamRoleId;

/** The permissions that {@code roleIds} and the loose {@code permissions} give together. */
public record ExpandPermissionsQuery(
    Collection<IamRoleId> roleIds, Collection<String> permissions) {}
