package org.ouracademy.ovejafiel.whatsapp.command.provision_templates;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** {@code organizationId} may be any node: the templates belong to its church. */
public record ProvisionTemplatesCommand(PartyId organizationId) {}
