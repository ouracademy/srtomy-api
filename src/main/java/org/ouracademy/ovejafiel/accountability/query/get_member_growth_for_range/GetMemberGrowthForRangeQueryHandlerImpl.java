package org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range.MemberGrowthRangeResult.Bucket;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range.MemberGrowthRangeResult.StateCounts;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit.Period;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Same replay as {@code GetMemberGrowthQueryHandlerImpl} (a person has a state per node, and in a
 * node with sub-nodes they count with the highest one), but sliced into whatever periods {@code
 * query.bucket()} produces for {@code [query.start(), query.end()]} instead of calendar months
 * anchored to "now". Kept separate from the original handler so the existing {@code /growth}
 * endpoint and its tests stay untouched.
 */
@Service
@RequiredArgsConstructor
class GetMemberGrowthForRangeQueryHandlerImpl implements GetMemberGrowthForRangeQueryHandler {

  private static final int NONE = 0;
  private static final int VISITOR = 1;
  private static final int ATTENDEE = 2;
  private static final int MEMBER = 3;

  private final MemberStateEventRepository repository;

  @Override
  @Transactional(readOnly = true)
  public MemberGrowthRangeResult handle(GetMemberGrowthForRangeQuery query) {
    if (query.start().isAfter(query.end())) {
      throw new DomainValidationException("La fecha de inicio no puede ser posterior a la de fin");
    }

    var startLdt = LocalDateTime.ofInstant(query.start(), ZoneOffset.UTC);
    var endLdt = LocalDateTime.ofInstant(query.end(), ZoneOffset.UTC);
    var periods = query.bucket().bucketsOver(startLdt, endLdt);
    var tallies = new ArrayList<Tally>(periods.size());
    for (var period : periods) {
      tallies.add(new Tally(period));
    }

    var stateByNode = new HashMap<UUID, Map<UUID, Integer>>();
    var overall = new HashMap<UUID, Integer>();
    var people = new Counter();
    var next = 0;
    var events =
        repository.findByOrganizations(
            query.organizationIds().stream().map(PartyId::value).toList());
    for (var event : events) {
      var at = event.getOccurredAt();
      // Close the periods that ended before this event with the totals they ended with.
      while (next < tallies.size() && !at.isBefore(tallies.get(next).period.endExclusive())) {
        tallies.get(next++).endWith(people);
      }

      var nodes = stateByNode.computeIfAbsent(event.getPersonId(), id -> new HashMap<>());
      if (event.getToState() == null) {
        nodes.remove(event.getOrganizationId());
      } else {
        nodes.put(event.getOrganizationId(), rankOf(event.getToState()));
      }
      var before = overall.getOrDefault(event.getPersonId(), NONE);
      var after = nodes.values().stream().mapToInt(Integer::intValue).max().orElse(NONE);
      overall.put(event.getPersonId(), after);
      people.move(before, after);

      if (!at.isBefore(startLdt) && next < tallies.size()) {
        tallies.get(next).record(before, after);
      }
    }
    while (next < tallies.size()) {
      tallies.get(next++).endWith(people);
    }
    return new MemberGrowthRangeResult(
        new StateCounts(people.visitors, people.attendees, people.members),
        tallies.stream().map(Tally::toBucket).toList());
  }

  private static int rankOf(String state) {
    return switch (state) {
      case "MEMBER" -> MEMBER;
      case "MEMBERSHIP_IN_PROGRESS" -> ATTENDEE;
      default -> VISITOR;
    };
  }

  /** How many people there are in each state right now. */
  private static final class Counter {
    long visitors;
    long attendees;
    long members;

    void move(int from, int to) {
      add(from, -1);
      add(to, 1);
    }

    private void add(int rank, int by) {
      switch (rank) {
        case VISITOR -> visitors += by;
        case ATTENDEE -> attendees += by;
        case MEMBER -> members += by;
        default -> {
          // not in the node
        }
      }
    }

    long total() {
      return visitors + attendees + members;
    }
  }

  /** What happened in one period and how many people there were when it ended. */
  private static final class Tally {
    final Period period;
    long joined;
    long becameAttendee;
    long becameMember;
    long left;
    long peopleAtEnd;
    long membersAtEnd;

    Tally(Period period) {
      this.period = period;
    }

    void record(int before, int after) {
      if (before == after) {
        return;
      }
      if (before == NONE) {
        joined++;
      } else if (after == NONE) {
        left++;
      } else if (after > before && after == ATTENDEE) {
        becameAttendee++;
      } else if (after > before && after == MEMBER) {
        becameMember++;
      }
    }

    void endWith(Counter people) {
      peopleAtEnd = people.total();
      membersAtEnd = people.members;
    }

    Bucket toBucket() {
      return new Bucket(
          period.start().toLocalDate(),
          period.endExclusive().minusDays(1).toLocalDate(),
          period.label(),
          joined,
          becameAttendee,
          becameMember,
          left,
          peopleAtEnd,
          membersAtEnd);
    }
  }
}
