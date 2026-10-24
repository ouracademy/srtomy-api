package org.ouracademy.srtomy.accountability.command.rename_position;

import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;

public record RenamePositionCommand(AccountabilityTypeId positionId, String name) {}
