package org.ouracademy.ovejafiel.directory.query.get_church_directory;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Every node of the church {@code churchId}, with its basic information, for {@code subject}. */
public record GetChurchDirectoryQuery(String subject, PartyId churchId) {}
