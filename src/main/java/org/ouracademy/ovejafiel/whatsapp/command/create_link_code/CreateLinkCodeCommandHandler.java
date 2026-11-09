package org.ouracademy.ovejafiel.whatsapp.command.create_link_code;

/**
 * Makes the code a signed-in person sends from their WhatsApp to the church number. It is only
 * given to someone who has a person in that church, and only its hash is kept.
 */
public interface CreateLinkCodeCommandHandler {
  CreateLinkCodeResult handle(CreateLinkCodeCommand command);
}
