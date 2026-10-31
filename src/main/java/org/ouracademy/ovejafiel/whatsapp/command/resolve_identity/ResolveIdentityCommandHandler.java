package org.ouracademy.ovejafiel.whatsapp.command.resolve_identity;

/**
 * Finds who writes by their phone and gives the agent a token to act as them. Nothing is trusted
 * from the message itself: the number comes from WhatsApp and the church from the number it was
 * sent to.
 */
public interface ResolveIdentityCommandHandler {
  ResolveIdentityResult handle(ResolveIdentityCommand command);
}
