package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;

/**
 * How the number of the church will be connected.
 *
 * @param mode COEXISTENCE keeps the WhatsApp Business app working next to the API; DEDICATED leaves
 *     the number for the API only.
 */
public record SetupLinkRequest(@NotNull ConnectionMode mode) {}
