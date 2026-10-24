package org.ouracademy.srtomy.iam.query.expand_permissions;

import java.util.List;

/** Distinct and sorted. Roles that no longer exist or were deleted give nothing. */
public record ExpandPermissionsResult(List<String> permissions) {}
