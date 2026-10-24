package org.ouracademy.srtomy.organization.command;

import org.ouracademy.srtomy.organization.domain.MemberVisibility;
import org.ouracademy.srtomy.organization.domain.SocialLinks;

/**
 * The descriptive, user-editable fields shared by every organization kind. {@code schedule} is
 * overwritten as given; a null {@code memberVisibility} or {@code showOrgChart} keeps the current
 * value (the default for a new node: public, with the org chart).
 */
public record OrganizationInfo(
    String name,
    String description,
    String address,
    String phoneNumber,
    SocialLinks socialLinks,
    String schedule,
    MemberVisibility memberVisibility,
    Boolean showOrgChart) {

  public OrganizationInfo(
      String name,
      String description,
      String address,
      String phoneNumber,
      SocialLinks socialLinks) {
    this(name, description, address, phoneNumber, socialLinks, null, null, null);
  }

  public OrganizationInfo(String name, String description, String address, String phoneNumber) {
    this(name, description, address, phoneNumber, SocialLinks.NONE);
  }
}
