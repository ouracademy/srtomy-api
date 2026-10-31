package org.ouracademy.ovejafiel.whatsapp.command.request_setup_link;

import java.time.Instant;

public record RequestSetupLinkResult(String url, Instant expiresAt) {}
