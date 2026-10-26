package org.ouracademy.srtomy.invitation.command.create_invitation;

import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

/**
 * The email is queued with the invitation; ask the outbox with {@code emailMessageId} how it went.
 */
public record CreateInvitationResult(InvitationResult invitation, OutboxMessageId emailMessageId) {}
