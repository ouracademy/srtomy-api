package org.ouracademy.srtomy.iam.command.update_role;

import java.util.List;
import org.ouracademy.srtomy.iam.domain.IamRoleId;

public record UpdateRoleCommand(
    String subject, IamRoleId roleId, String name, String description, List<String> permissions) {}
