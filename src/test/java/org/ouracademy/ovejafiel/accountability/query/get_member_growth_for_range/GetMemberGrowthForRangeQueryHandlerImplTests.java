package org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository.EventRow;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit;

class GetMemberGrowthForRangeQueryHandlerImplTests {

  MemberStateEventRepository repository = mock(MemberStateEventRepository.class);
  GetMemberGrowthForRangeQueryHandlerImpl handler =
      new GetMemberGrowthForRangeQueryHandlerImpl(repository);

  PartyId node = PartyId.generate();
  List<EventRow> history = new ArrayList<>();

  /** One event, at the given instant. */
  void event(UUID person, PartyId in, LocalDateTime at, String from, String to) {
    var row = mock(EventRow.class);
    when(row.getPersonId()).thenReturn(person);
    when(row.getOrganizationId()).thenReturn(in.value());
    when(row.getFromState()).thenReturn(from);
    when(row.getToState()).thenReturn(to);
    when(row.getOccurredAt()).thenReturn(at);
    history.add(row);
  }

  MemberGrowthRangeResult growth(Instant start, Instant end, BucketUnit bucket, PartyId... nodes) {
    when(repository.findByOrganizations(any())).thenReturn(history);
    return handler.handle(new GetMemberGrowthForRangeQuery(List.of(nodes), start, end, bucket));
  }

  @Test
  void dayBucketsForAShortRange() {
    var ana = UUID.randomUUID();
    var luis = UUID.randomUUID();
    event(ana, node, LocalDateTime.of(2026, 10, 1, 10, 0), null, "VISITOR");
    event(ana, node, LocalDateTime.of(2026, 10, 2, 10, 0), "VISITOR", "MEMBER");
    event(luis, node, LocalDateTime.of(2026, 10, 3, 10, 0), null, "VISITOR");

    var result =
        growth(
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-10-03T23:59:59Z"),
            BucketUnit.DAY,
            node);

    assertThat(result.buckets())
        .extracting(MemberGrowthRangeResult.Bucket::label)
        .containsExactly("2026-10-01", "2026-10-02", "2026-10-03");
    assertThat(result.buckets().get(0).joined()).isEqualTo(1);
    assertThat(result.buckets().get(1).becameMember()).isEqualTo(1);
    assertThat(result.buckets().get(2).joined()).isEqualTo(1);
    assertThat(result.buckets().get(2).peopleAtEnd()).isEqualTo(2);
    assertThat(result.current().total()).isEqualTo(2);
  }

  @Test
  void weekBucketsForAMediumRange() {
    var ana = UUID.randomUUID();
    event(ana, node, LocalDateTime.of(2026, 10, 1, 10, 0), null, "VISITOR");
    event(ana, node, LocalDateTime.of(2026, 10, 10, 10, 0), "VISITOR", "MEMBER");

    var result =
        growth(
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-10-14T23:59:59Z"),
            BucketUnit.WEEK,
            node);

    assertThat(result.buckets())
        .extracting(MemberGrowthRangeResult.Bucket::label)
        .containsExactly("2026-10-01", "2026-10-08");
    assertThat(result.buckets().get(0).joined()).isEqualTo(1);
    assertThat(result.buckets().get(1).becameMember()).isEqualTo(1);
  }

  @Test
  void monthBucketsForALongRange() {
    var ana = UUID.randomUUID();
    event(ana, node, LocalDateTime.of(2026, 8, 15, 10, 0), null, "VISITOR");
    event(ana, node, LocalDateTime.of(2026, 10, 15, 10, 0), "VISITOR", "MEMBER");

    var result =
        growth(
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-10-31T23:59:59Z"),
            BucketUnit.MONTH,
            node);

    assertThat(result.buckets())
        .extracting(MemberGrowthRangeResult.Bucket::label)
        .containsExactly("2026-08", "2026-09", "2026-10");
    assertThat(result.buckets().get(0).joined()).isEqualTo(1);
    assertThat(result.buckets().get(1).joined()).isZero();
    assertThat(result.buckets().get(2).becameMember()).isEqualTo(1);
  }

  @Test
  void aBucketWithNoEventsIsStillThereWithZeroes() {
    var result =
        growth(
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-10-03T23:59:59Z"),
            BucketUnit.DAY,
            node);

    assertThat(result.buckets()).hasSize(3);
    assertThat(result.buckets())
        .allSatisfy(
            bucket -> {
              assertThat(bucket.joined()).isZero();
              assertThat(bucket.left()).isZero();
              assertThat(bucket.peopleAtEnd()).isZero();
            });
  }

  @Test
  void anEventExactlyOnABucketBoundaryGoesToTheBucketItOpens() {
    var ana = UUID.randomUUID();
    // Midnight of the 2nd is the boundary between day 1 and day 2: it belongs to day 2.
    event(ana, node, LocalDateTime.of(2026, 10, 2, 0, 0, 0), null, "VISITOR");

    var result =
        growth(
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-10-03T23:59:59Z"),
            BucketUnit.DAY,
            node);

    assertThat(result.buckets().get(0).joined()).isZero();
    assertThat(result.buckets().get(1).joined()).isEqualTo(1);
    assertThat(result.buckets().get(2).joined()).isZero();
  }
}
