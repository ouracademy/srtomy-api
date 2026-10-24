package org.ouracademy.srtomy.iam.command.delete_role;

import org.ouracademy.srtomy.iam.domain.IamRoleId;

public record DeleteRoleCommand(IamRoleId roleId) {}
