package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp.LinkWhatsAppResult;

public record LinkWhatsAppResponse(LinkWhatsAppResult.Status status, String churchName) {}
