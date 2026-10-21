package org.ouracademy.srtomy.complaint.command.create_complaint;

/**
 * Files a complaint from the public complaint form. {@code dni} and {@code address} are optional.
 */
public record CreateComplaintCommand(
    String firstName, String lastName, String dni, String address, String email, String comment) {}
