package org.ouracademy.ovejafiel.directory.query.get_directory_entry;

import org.ouracademy.ovejafiel.party.domain.PartyId;

public record GetDirectoryEntryQuery(String subject, PartyId nodeId) {}
