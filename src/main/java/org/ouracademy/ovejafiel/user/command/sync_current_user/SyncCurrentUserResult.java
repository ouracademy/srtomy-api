package org.ouracademy.ovejafiel.user.command.sync_current_user;

import org.ouracademy.ovejafiel.user.query.UserResult;

public record SyncCurrentUserResult(UserResult user, boolean created) {}
