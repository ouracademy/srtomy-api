package org.ouracademy.srtomy.directory.query.get_directory_entry;

import org.ouracademy.srtomy.party.domain.PartyId;

public record GetDirectoryEntryQuery(String subject, PartyId nodeId) {}
