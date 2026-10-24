package org.ouracademy.srtomy.user.command.sync_current_user;

/** Creates the user for {@code subject} if it is new, and refreshes its email. */
public record SyncCurrentUserCommand(String subject, String email, boolean emailVerified) {}
