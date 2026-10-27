package org.ouracademy.ovejafiel.organization.domain;

/**
 * Who can see the list of people of a church, location, group or ministry. The basic information of
 * the node (name, description, schedule, leaders) is always in the church directory.
 */
public enum MemberVisibility {
  /** Anyone with the link of the public page, signed in or not. */
  PUBLIC,
  /** Any user with access somewhere in the church. */
  CHURCH,
  /** The members of the node and whoever can read its people ({@code members.read}). */
  MEMBERS,
  /** Only whoever can read its people ({@code members.read}). */
  LEADERS
}
