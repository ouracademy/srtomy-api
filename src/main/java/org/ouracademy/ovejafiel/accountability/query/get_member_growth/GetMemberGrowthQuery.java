package org.ouracademy.ovejafiel.accountability.query.get_member_growth;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * How the people of a node are doing, over {@code organizationIds}: the node and everything below
 * it. Someone counts once, with the highest state they have in any of those nodes. Shows the last
 * {@code months}.
 */
public record GetMemberGrowthQuery(List<PartyId> organizationIds, int months) {}
