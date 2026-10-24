package org.ouracademy.srtomy.iam.command.grant_access;

/** {@code created} is false when the user already had that role on the node. */
public record GrantAccessResult(boolean created) {}
