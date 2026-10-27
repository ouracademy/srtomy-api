package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;

/** Who can open the event through its link. */
public record EventSharingRequest(@NotNull GeneralAccess generalAccess) {}
