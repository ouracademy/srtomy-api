package org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range;

import java.time.Instant;
import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit;

/**
 * How the people of {@code organizationIds} (a node and everything below it) are doing between
 * {@code start} and {@code end}, sliced into {@code bucket}-sized periods. Same idea as {@code
 * GetMemberGrowthQuery}, but over an explicit window instead of the last N calendar months.
 */
public record GetMemberGrowthForRangeQuery(
    List<PartyId> organizationIds, Instant start, Instant end, BucketUnit bucket) {}
