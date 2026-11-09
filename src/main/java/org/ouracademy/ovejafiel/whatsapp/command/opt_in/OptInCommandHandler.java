package org.ouracademy.ovejafiel.whatsapp.command.opt_in;

/** Turns the WhatsApp notices back on for every person of the church with that phone. */
public interface OptInCommandHandler {
  OptInResult handle(OptInCommand command);
}
