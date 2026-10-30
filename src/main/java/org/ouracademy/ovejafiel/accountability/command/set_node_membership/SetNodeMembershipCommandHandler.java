package org.ouracademy.ovejafiel.accountability.command.set_node_membership;

/**
 * Fails with {@code DomainValidationException} (400) when the state is not a member progress state
 * or when someone who holds a position there is given a state other than member.
 */
public interface SetNodeMembershipCommandHandler {
  SetNodeMembershipResult handle(SetNodeMembershipCommand command);
}
