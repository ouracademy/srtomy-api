package org.ouracademy.ovejafiel.iam.command.grant_role;

/** {@code created} is false when the same binding already existed (granting is idempotent). */
public record GrantRoleResult(boolean created) {}
