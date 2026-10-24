package org.ouracademy.srtomy.user.command.sync_current_user;

import org.ouracademy.srtomy.user.query.UserResult;

public record SyncCurrentUserResult(UserResult user, boolean created) {}
