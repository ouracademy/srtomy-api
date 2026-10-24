package org.ouracademy.srtomy.iam.command.create_role;

import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;

public record CreateRoleCommand(
    String subject, PartyId churchId, String name, String description, List<String> permissions) {}
