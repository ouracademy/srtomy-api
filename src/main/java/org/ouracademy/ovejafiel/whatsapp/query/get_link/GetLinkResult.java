package org.ouracademy.ovejafiel.whatsapp.query.get_link;

import java.time.LocalDateTime;

/**
 * @param maskedPhone the number with all but its last four digits hidden; null when not linked
 */
public record GetLinkResult(boolean linked, String maskedPhone, LocalDateTime verifiedAt) {}
