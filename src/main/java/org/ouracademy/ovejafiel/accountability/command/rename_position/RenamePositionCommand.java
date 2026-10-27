package org.ouracademy.ovejafiel.accountability.command.rename_position;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;

public record RenamePositionCommand(AccountabilityTypeId positionId, String name) {}
