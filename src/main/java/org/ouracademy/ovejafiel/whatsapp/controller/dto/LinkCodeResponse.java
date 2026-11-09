package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import java.time.LocalDateTime;

/** What the portal shows: "send this code from your WhatsApp to this number". */
public record LinkCodeResponse(String code, LocalDateTime expiresAt, String displayNumber) {}
