package org.ouracademy.ovejafiel.whatsapp.command.opt_out;

/** Stops the WhatsApp notices to every person of the church with that phone. */
public interface OptOutCommandHandler {
  OptOutResult handle(OptOutCommand command);
}
