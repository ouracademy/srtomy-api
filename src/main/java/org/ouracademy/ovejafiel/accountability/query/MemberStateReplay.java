package org.ouracademy.ovejafiel.accountability.query;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.ouracademy.ovejafiel.accountability.repository.MemberStateEventRepository.EventRow;

/**
 * Replays the history of states: a person has a state per node, and in a node with sub-nodes they
 * count with the highest one. Replaying is what lets the same event feed a group's numbers and its
 * church's, and tell how many people and members there were when each period ended. Shared by the
 * growth queries, which only differ in how they slice time.
 */
public final class MemberStateReplay {

  private static final int NONE = 0;
  private static final int VISITOR = 1;
  private static final int ATTENDEE = 2;
  private static final int MEMBER = 3;

  private final Map<UUID, Map<UUID, Integer>> stateByNode = new HashMap<>();
  private final Map<UUID, Integer> overall = new HashMap<>();
  private final Counter people = new Counter();

  /** The highest state a person had before an event and after it, as ranks. */
  public record Change(int before, int after) {}

  /** Applies the next event, in the order they happened, and says how it moved the person. */
  public Change apply(EventRow event) {
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
    return new Change(before, after);
  }

  /** How many people there are in each state after the events applied so far. */
  public Counter people() {
    return people;
  }

  private static int rankOf(String state) {
    return switch (state) {
      case "MEMBER" -> MEMBER;
      case "MEMBERSHIP_IN_PROGRESS" -> ATTENDEE;
      default -> VISITOR;
    };
  }

  /** How many people there are in each state right now. */
  public static final class Counter {
    private long visitors;
    private long attendees;
    private long members;

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

    public long visitors() {
      return visitors;
    }

    public long attendees() {
      return attendees;
    }

    public long members() {
      return members;
    }

    public long total() {
      return visitors + attendees + members;
    }
  }

  /** What happened in one period and how many people there were when it ended. */
  public static final class Tally {
    private long joined;
    private long becameAttendee;
    private long becameMember;
    private long left;
    private long peopleAtEnd;
    private long membersAtEnd;

    public void register(Change change) {
      var before = change.before();
      var after = change.after();
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

    public void endWith(Counter counter) {
      peopleAtEnd = counter.total();
      membersAtEnd = counter.members();
    }

    public long joined() {
      return joined;
    }

    public long becameAttendee() {
      return becameAttendee;
    }

    public long becameMember() {
      return becameMember;
    }

    public long left() {
      return left;
    }

    public long peopleAtEnd() {
      return peopleAtEnd;
    }

    public long membersAtEnd() {
      return membersAtEnd;
    }
  }
}
