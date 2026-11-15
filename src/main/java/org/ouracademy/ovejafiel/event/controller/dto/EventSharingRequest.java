package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;

/**
 * Who can open the event through its link. With {@code applyToProgram} the same access is given to
 * every sub-event of a mega event, so the public program does not lose sub-events that were left
 * with another one.
 *
 * @param generalAccess Who can open the event besides the people it is shared with.
 * @param applyToProgram true to give the same access to every sub-event of a mega event. Optional.
 */
public record EventSharingRequest(@NotNull GeneralAccess generalAccess, Boolean applyToProgram) {}
