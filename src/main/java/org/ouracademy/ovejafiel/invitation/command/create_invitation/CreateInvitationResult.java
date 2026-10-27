package org.ouracademy.ovejafiel.invitation.command.create_invitation;

import org.ouracademy.ovejafiel.invitation.query.InvitationResult;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

/**
 * The email is queued with the invitation; ask the outbox with {@code emailMessageId} how it went.
 */
public record CreateInvitationResult(InvitationResult invitation, OutboxMessageId emailMessageId) {}
