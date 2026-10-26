package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.LinkRole;

/** Who can open the event through its link, and what that allows. */
public record EventSharingRequest(
    @NotNull GeneralAccess generalAccess, @NotNull LinkRole linkRole) {}
