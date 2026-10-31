package org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp;

/** Someone sent {@code code} from {@code from} to the church number {@code phoneNumberId}. */
public record LinkWhatsAppCommand(String phoneNumberId, String from, String code) {}
