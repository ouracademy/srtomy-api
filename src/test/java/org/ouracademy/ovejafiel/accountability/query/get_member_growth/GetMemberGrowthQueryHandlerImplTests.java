package org.ouracademy.ovejafiel.accountability.query.get_member_growth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository.EventRow;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class GetMemberGrowthQueryHandlerImplTests {

  MemberStateEventRepository repository = mock(MemberStateEventRepository.class);
  GetMemberGrowthQueryHandlerImpl handler = new GetMemberGrowthQueryHandlerImpl(repository);

  PartyId node = PartyId.generate();
  PartyId subNode = PartyId.generate();
  YearMonth thisMonth = YearMonth.from(LocalDate.now());
  List<EventRow> history = new ArrayList<>();

  /** One event, at the given day of the month. */
  void event(UUID person, PartyId in, YearMonth month, String from, String to) {
    var row = mock(EventRow.class);
    when(row.getPersonId()).thenReturn(person);
    when(row.getOrganizationId()).thenReturn(in.value());
    when(row.getFromState()).thenReturn(from);
    when(row.getToState()).thenReturn(to);
    when(row.getOccurredAt())
        .thenReturn(LocalDateTime.of(month.getYear(), month.getMonth(), 5, 10, 0));
    history.add(row);
  }

  MemberGrowthResult growth(int months, PartyId... nodes) {
    when(repository.findByOrganizations(any())).thenReturn(history);
    return handler.handle(new GetMemberGrowthQuery(List.of(nodes), months));
  }

  @Test
  void countsThePeopleByStateTodayFromTheirLatestEvents() {
    var ana = UUID.randomUUID();
    var luis = UUID.randomUUID();
    var eva = UUID.randomUUID();
    event(ana, node, thisMonth, null, "VISITOR");
    event(ana, node, thisMonth, "VISITOR", "MEMBERSHIP_IN_PROGRESS");
    event(luis, node, thisMonth, null, "MEMBER");
    event(eva, node, thisMonth, null, "VISITOR");

    var current = growth(3, node).current();

    assertThat(current.visitors()).isEqualTo(1);
    assertThat(current.attendees()).isEqualTo(1);
    assertThat(current.members()).isEqualTo(1);
    assertThat(current.total()).isEqualTo(3);
  }

  @Test
  void everyMonthOfTheRangeIsThereOldestFirstEvenWithoutEvents() {
    var months = growth(4, node).months();

    assertThat(months)
        .extracting(MemberGrowthResult.Month::month)
        .containsExactly(
            thisMonth.minusMonths(3).toString(),
            thisMonth.minusMonths(2).toString(),
            thisMonth.minusMonths(1).toString(),
            thisMonth.toString());
    assertThat(months).allSatisfy(month -> assertThat(month.joined()).isZero());
  }

  @Test
  void splitsEachMonthIntoJoinedProgressedAndLeft() {
    var ana = UUID.randomUUID();
    var luis = UUID.randomUUID();
    var eva = UUID.randomUUID();
    var teo = UUID.randomUUID();
    var last = thisMonth.minusMonths(1);
    // Four people join; three of them were there from last month.
    event(ana, node, last, null, "VISITOR");
    event(luis, node, last, null, "VISITOR");
    event(eva, node, last, null, "VISITOR");
    event(ana, node, thisMonth, "VISITOR", "MEMBERSHIP_IN_PROGRESS");
    event(luis, node, thisMonth, "VISITOR", "MEMBER");
    event(eva, node, thisMonth, "VISITOR", null);
    event(teo, node, thisMonth, null, "VISITOR");

    var months = growth(2, node).months();

    var before = months.get(0);
    assertThat(before.joined()).isEqualTo(3);
    assertThat(before.peopleAtEnd()).isEqualTo(3);
    var now = months.get(1);
    assertThat(now.joined()).isEqualTo(1);
    assertThat(now.becameAttendee()).isEqualTo(1);
    assertThat(now.becameMember()).isEqualTo(1);
    assertThat(now.left()).isEqualTo(1);
  }

  @Test
  void tellsHowManyPeopleAndMembersThereWereWhenEachMonthEnded() {
    var ana = UUID.randomUUID();
    var luis = UUID.randomUUID();
    var eva = UUID.randomUUID();
    var last = thisMonth.minusMonths(1);
    // Last month: Ana and Luis join, Luis straight as a member.
    event(ana, node, last, null, "VISITOR");
    event(luis, node, last, null, "MEMBER");
    // This month: Ana becomes a member, Eva joins and Luis leaves.
    event(ana, node, thisMonth, "VISITOR", "MEMBER");
    event(eva, node, thisMonth, null, "VISITOR");
    event(luis, node, thisMonth, "MEMBER", null);

    var months = growth(3, node).months();

    // Before any of it.
    assertThat(months.get(0).peopleAtEnd()).isZero();
    assertThat(months.get(1).peopleAtEnd()).isEqualTo(2);
    assertThat(months.get(1).membersAtEnd()).isEqualTo(1);
    assertThat(months.get(2).peopleAtEnd()).isEqualTo(2);
    assertThat(months.get(2).membersAtEnd()).isEqualTo(1);
  }

  @Test
  void aPersonInSeveralNodesCountsOnceWithTheirHighestState() {
    var ana = UUID.randomUUID();
    // Member of the church, attendee of the youth group below it.
    event(ana, node, thisMonth, null, "MEMBER");
    event(ana, subNode, thisMonth, null, "MEMBERSHIP_IN_PROGRESS");

    var result = growth(1, node, subNode);

    assertThat(result.current().total()).isEqualTo(1);
    assertThat(result.current().members()).isEqualTo(1);
    // She joined once, not once per node, and was never "promoted" by being in the second one.
    var month = result.months().get(0);
    assertThat(month.joined()).isEqualTo(1);
    assertThat(month.becameAttendee()).isZero();
  }

  @Test
  void leavingTheOnlyNodeIsLeavingButLeavingOneOfSeveralIsNot() {
    var ana = UUID.randomUUID();
    var luis = UUID.randomUUID();
    event(ana, node, thisMonth, null, "VISITOR");
    event(ana, subNode, thisMonth, null, "MEMBER");
    event(ana, subNode, thisMonth, "MEMBER", null);
    event(luis, subNode, thisMonth, null, "VISITOR");
    event(luis, subNode, thisMonth, "VISITOR", null);

    var result = growth(1, node, subNode);

    assertThat(result.current().total()).isEqualTo(1);
    var month = result.months().get(0);
    assertThat(month.joined()).isEqualTo(2);
    // Only Luis is gone from the church.
    assertThat(month.left()).isEqualTo(1);
  }

  @Test
  void eventsBeforeTheRangeShapeTheStateButAreNotCounted() {
    var ana = UUID.randomUUID();
    event(ana, node, thisMonth.minusMonths(10), null, "VISITOR");

    var result = growth(2, node);

    assertThat(result.current().visitors()).isEqualTo(1);
    assertThat(result.months()).allSatisfy(month -> assertThat(month.joined()).isZero());
    assertThat(result.months()).allSatisfy(month -> assertThat(month.peopleAtEnd()).isEqualTo(1));
  }
}
