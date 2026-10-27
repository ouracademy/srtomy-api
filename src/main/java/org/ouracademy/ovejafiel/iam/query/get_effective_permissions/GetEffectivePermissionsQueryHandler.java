package org.ouracademy.ovejafiel.iam.query.get_effective_permissions;

public interface GetEffectivePermissionsQueryHandler {
  EffectivePermissionsResult handle(GetEffectivePermissionsQuery query);
}
