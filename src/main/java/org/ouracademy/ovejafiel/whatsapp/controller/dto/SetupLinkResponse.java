package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import java.time.Instant;

public record SetupLinkResponse(String url, Instant expiresAt) {}
