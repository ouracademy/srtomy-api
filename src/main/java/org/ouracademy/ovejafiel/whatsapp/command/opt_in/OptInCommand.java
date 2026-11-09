package org.ouracademy.ovejafiel.whatsapp.command.opt_in;

/** Someone answered ALTA: {@code from} wrote to the church number {@code phoneNumberId}. */
public record OptInCommand(String phoneNumberId, String from) {}
