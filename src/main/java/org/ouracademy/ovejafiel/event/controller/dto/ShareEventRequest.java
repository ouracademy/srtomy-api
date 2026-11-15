package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.event.domain.ShareRole;

/**
 * Shares an event with one person.
 *
 * @param email Email of the person the event is shared with.
 * @param role What they can do: view or edit.
 */
public record ShareEventRequest(@NotBlank @Email String email, @NotNull ShareRole role) {}
