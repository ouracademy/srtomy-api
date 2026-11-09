package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import java.time.LocalDateTime;

public record WhatsAppLinkResponse(boolean linked, String maskedPhone, LocalDateTime verifiedAt) {}
