package org.ouracademy.ovejafiel.person.command.set_whatsapp_consent;

import java.util.Collection;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record SetWhatsAppConsentCommand(Collection<PartyId> personIds, boolean consent) {}
