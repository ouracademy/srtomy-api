package org.ouracademy.ovejafiel.whatsapp.command.opt_out;

/** Someone answered STOP: {@code from} wrote to the church number {@code phoneNumberId}. */
public record OptOutCommand(String phoneNumberId, String from) {}
