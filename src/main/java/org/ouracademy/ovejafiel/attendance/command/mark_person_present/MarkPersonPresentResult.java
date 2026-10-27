package org.ouracademy.ovejafiel.attendance.command.mark_person_present;

/** {@code alreadyMarked} when they were already in the attendance; nothing changed. */
public record MarkPersonPresentResult(boolean alreadyMarked) {}
