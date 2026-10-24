package org.ouracademy.srtomy.accountability.query;

/** A position with how many people hold it (in any node of the church). */
public record PositionSummaryResult(PositionResult position, long holders) {}
