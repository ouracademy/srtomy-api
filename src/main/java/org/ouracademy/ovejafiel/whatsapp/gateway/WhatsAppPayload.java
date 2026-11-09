package org.ouracademy.ovejafiel.whatsapp.gateway;

import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;

/** What is stored in the payload of an outbox message of the WHATSAPP channel. */
public record WhatsAppPayload(
    UUID organizationId, WhatsAppTemplate template, List<String> variables) {}
