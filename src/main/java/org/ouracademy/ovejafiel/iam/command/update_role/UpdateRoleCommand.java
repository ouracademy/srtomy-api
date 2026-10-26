package org.ouracademy.ovejafiel.iam.command.update_role;

import java.util.List;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;

public record UpdateRoleCommand(
    String subject, IamRoleId roleId, String name, String description, List<String> permissions) {}
