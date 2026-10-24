package org.ouracademy.srtomy.accountability.command.delete_position;

import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;

/** Soft-deletes a position and unassigns it from everyone who held it. */
public record DeletePositionCommand(AccountabilityTypeId positionId) {}
