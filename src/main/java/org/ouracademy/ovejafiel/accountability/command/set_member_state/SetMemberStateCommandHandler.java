package org.ouracademy.ovejafiel.accountability.command.set_member_state;

/**
 * Fails with {@code DomainValidationException} (400) when the type is not a member progress state.
 */
public interface SetMemberStateCommandHandler {
  SetMemberStateResult handle(SetMemberStateCommand command);
}
