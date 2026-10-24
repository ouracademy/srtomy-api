package org.ouracademy.srtomy.accountability.command.set_position_access;

import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.iam.domain.IamRoleId;

/**
 * Sets the access level of a position ({@code null} makes it a plain label). Both the current and
 * the new level are checked against {@code subject}: nobody changes a position that gives more
 * access than they hold, and nobody gives it more.
 */
public record SetPositionAccessCommand(
    String subject, AccountabilityTypeId positionId, IamRoleId accessRoleId) {}
