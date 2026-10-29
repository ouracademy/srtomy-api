package org.ouracademy.ovejafiel.person.command.delete_person;

public record DeletePersonResult(Outcome outcome) {

  public enum Outcome {
    /** The person was removed from the church. */
    DELETED,
    /** The person was removed from the node only; they still belong to the church. */
    REMOVED_FROM_NODE,
    /** There is no such person (or they were already removed from the church). */
    NOT_FOUND,
    /**
     * A user represents the person and the caller did not confirm unlinking them; nothing changed.
     */
    REQUIRES_USER_UNLINK
  }
}
