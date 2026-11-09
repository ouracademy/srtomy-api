package org.ouracademy.ovejafiel.whatsapp.command.create_link_code;

import java.time.LocalDateTime;

/**
 * @param code what the person sends, shown once
 * @param displayNumber the church number to send it to; null while it is not known
 */
public record CreateLinkCodeResult(String code, LocalDateTime expiresAt, String displayNumber) {}
