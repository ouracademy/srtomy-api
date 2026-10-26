package org.ouracademy.ovejafiel.organization.command.create_church;

import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;

public record CreateChurchResult(
    FindOrganizationResult organization, FindOrganizationResult location) {}
