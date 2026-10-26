package org.ouracademy.ovejafiel.accountability.query;

import java.util.List;

/**
 * A position with the permissions it gives in all ({@code effectivePermissions}: roles plus loose
 * ones, distinct and sorted) and the people who hold it in any node of the church.
 */
public record PositionSummaryResult(
    PositionResult position,
    List<String> effectivePermissions,
    List<PositionHolderResult> holders) {}
