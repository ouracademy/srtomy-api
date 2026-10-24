package org.ouracademy.srtomy.iam.command.add_owner;

/** {@code created} is false when that user was already an Owner. */
public record AddOwnerResult(boolean created) {}
