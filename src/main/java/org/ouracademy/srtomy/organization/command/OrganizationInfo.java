package org.ouracademy.srtomy.organization.command;

/** The descriptive, user-editable fields shared by every organization kind. */
public record OrganizationInfo(
    String name, String description, String address, String phoneNumber) {}
