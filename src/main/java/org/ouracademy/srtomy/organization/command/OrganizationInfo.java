package org.ouracademy.srtomy.organization.command;

import org.ouracademy.srtomy.organization.domain.SocialLinks;

/** The descriptive, user-editable fields shared by every organization kind. */
public record OrganizationInfo(
    String name, String description, String address, String phoneNumber, SocialLinks socialLinks) {

  public OrganizationInfo(String name, String description, String address, String phoneNumber) {
    this(name, description, address, phoneNumber, SocialLinks.NONE);
  }
}
