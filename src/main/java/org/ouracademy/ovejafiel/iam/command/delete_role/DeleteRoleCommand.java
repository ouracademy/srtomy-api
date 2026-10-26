package org.ouracademy.ovejafiel.iam.command.delete_role;

import org.ouracademy.ovejafiel.iam.domain.IamRoleId;

public record DeleteRoleCommand(IamRoleId roleId) {}
