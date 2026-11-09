package org.ouracademy.ovejafiel.event.query.find_events_in_range;

import java.time.Instant;
import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Single events of {@code organizationIds} (already resolved by the caller, e.g. a node and
 * everything below it) that overlap {@code [start, end]}. Unlike {@code SearchEventsQuery}, this
 * does not resolve a subtree itself: callers that already have the node ids (such as a dashboard
 * query spanning several modules) use this to avoid resolving it twice.
 */
public record FindEventsInRangeQuery(List<PartyId> organizationIds, Instant start, Instant end) {}
