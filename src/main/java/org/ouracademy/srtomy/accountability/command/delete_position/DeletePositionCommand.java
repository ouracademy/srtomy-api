package org.ouracademy.srtomy.accountability.command.delete_position;

import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;

/**
 * Soft-deletes a position and unassigns it from everyone who held it. Deleting one that gives
 * access takes that access away, so it is checked against {@code subject} like changing its level.
 */
public record DeletePositionCommand(String subject, AccountabilityTypeId positionId) {}
