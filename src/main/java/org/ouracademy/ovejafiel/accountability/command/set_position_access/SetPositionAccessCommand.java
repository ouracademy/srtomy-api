package org.ouracademy.ovejafiel.accountability.command.set_position_access;

import java.util.Set;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;

/**
 * Sets what a position gives: the permissions of {@code roleIds} plus the loose {@code permissions}
 * (both empty makes it a plain label). Both what it gave and what it will give are checked against
 * {@code subject}: nobody changes a position that gives more than they hold, and nobody gives it
 * more.
 */
public record SetPositionAccessCommand(
    String subject,
    AccountabilityTypeId positionId,
    Set<IamRoleId> roleIds,
    Set<String> permissions) {}
