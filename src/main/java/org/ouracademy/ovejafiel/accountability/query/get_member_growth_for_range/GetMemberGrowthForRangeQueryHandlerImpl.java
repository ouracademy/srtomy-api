package org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.MemberStateReplay;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range.MemberGrowthRangeResult.Bucket;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range.MemberGrowthRangeResult.StateCounts;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit.Period;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The same replay as the {@code /growth} handler (see {@link MemberStateReplay}), but sliced into
 * whatever periods {@code query.bucket()} produces for {@code [query.start(), query.end()]} instead
 * of calendar months anchored to "now".
 */
@Service
@RequiredArgsConstructor
class GetMemberGrowthForRangeQueryHandlerImpl implements GetMemberGrowthForRangeQueryHandler {

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

    var replay = new MemberStateReplay();
    var people = replay.people();
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

      var change = replay.apply(event);
      if (!at.isBefore(startLdt) && next < tallies.size()) {
        tallies.get(next).counts.register(change);
      }
    }
    while (next < tallies.size()) {
      tallies.get(next++).endWith(people);
    }
    return new MemberGrowthRangeResult(
        new StateCounts(people.visitors(), people.attendees(), people.members()),
        tallies.stream().map(Tally::toBucket).toList());
  }

  /** One period and what happened in it. */
  private static final class Tally {
    final Period period;
    final MemberStateReplay.Tally counts = new MemberStateReplay.Tally();

    Tally(Period period) {
      this.period = period;
    }

    void endWith(MemberStateReplay.Counter people) {
      counts.endWith(people);
    }

    Bucket toBucket() {
      return new Bucket(
          period.start().toLocalDate(),
          period.endExclusive().minusDays(1).toLocalDate(),
          period.label(),
          counts.joined(),
          counts.becameAttendee(),
          counts.becameMember(),
          counts.left(),
          counts.peopleAtEnd(),
          counts.membersAtEnd());
    }
  }
}
