package org.ouracademy.ovejafiel.accountability.query.get_member_growth;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.MemberStateReplay;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.MemberGrowthResult.Month;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.MemberGrowthResult.StateCounts;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The history of states (see {@link MemberStateReplay}) sliced into calendar months up to now. */
@Service
@RequiredArgsConstructor
class GetMemberGrowthQueryHandlerImpl implements GetMemberGrowthQueryHandler {

  private final MemberStateEventRepository repository;

  @Override
  @Transactional(readOnly = true)
  public MemberGrowthResult handle(GetMemberGrowthQuery query) {
    var first = YearMonth.from(LocalDate.now()).minusMonths(query.months() - 1L);
    var months = new ArrayList<Tally>();
    for (var i = 0; i < query.months(); i++) {
      months.add(new Tally(first.plusMonths(i)));
    }

    var replay = new MemberStateReplay();
    var people = replay.people();
    var next = 0;
    var events =
        repository.findByOrganizations(
            query.organizationIds().stream().map(PartyId::value).toList());
    for (var event : events) {
      // Close the months that ended before this event with the totals they ended with.
      var month = YearMonth.from(event.getOccurredAt());
      while (next < months.size() && months.get(next).month.isBefore(month)) {
        months.get(next++).endWith(people);
      }

      var change = replay.apply(event);
      if (!month.isBefore(first)) {
        months.get((int) ChronoUnit.MONTHS.between(first, month)).counts.register(change);
      }
    }
    while (next < months.size()) {
      months.get(next++).endWith(people);
    }
    return new MemberGrowthResult(
        new StateCounts(people.visitors(), people.attendees(), people.members()),
        months.stream().map(Tally::toMonth).toList());
  }

  /** One month and what happened in it. */
  private static final class Tally {
    final YearMonth month;
    final MemberStateReplay.Tally counts = new MemberStateReplay.Tally();

    Tally(YearMonth month) {
      this.month = month;
    }

    void endWith(MemberStateReplay.Counter people) {
      counts.endWith(people);
    }

    Month toMonth() {
      return new Month(
          month.toString(),
          counts.joined(),
          counts.becameAttendee(),
          counts.becameMember(),
          counts.left(),
          counts.peopleAtEnd(),
          counts.membersAtEnd());
    }
  }
}
