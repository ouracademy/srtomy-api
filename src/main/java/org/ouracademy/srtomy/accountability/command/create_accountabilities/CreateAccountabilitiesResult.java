package org.ouracademy.srtomy.accountability.command.create_accountabilities;

import java.util.List;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;

/** Ids of the created accountabilities, in the same order as the command's items. */
public record CreateAccountabilitiesResult(List<AccountabilityId> ids) {}
