package org.ouracademy.ovejafiel.iam.query.get_effective_permissions;

import java.util.Set;
import org.ouracademy.ovejafiel.iam.domain.Permission;

public record EffectivePermissionsResult(Set<Permission> permissions) {}
