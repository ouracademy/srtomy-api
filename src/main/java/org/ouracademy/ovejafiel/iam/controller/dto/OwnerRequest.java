package org.ouracademy.ovejafiel.iam.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Who becomes an Owner.
 *
 * @param email Email of the user who becomes an Owner; they must already have an account.
 */
public record OwnerRequest(@NotBlank @Email String email) {}
