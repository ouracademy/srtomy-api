package org.ouracademy.srtomy.iam.query.get_effective_permissions;

import java.util.Set;
import org.ouracademy.srtomy.iam.domain.Permission;

public record EffectivePermissionsResult(Set<Permission> permissions) {}
