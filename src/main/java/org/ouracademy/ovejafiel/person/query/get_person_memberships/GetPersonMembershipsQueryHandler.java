package org.ouracademy.ovejafiel.person.query.get_person_memberships;

import java.util.List;

/** The nodes a person belongs to and how they stand in each, sorted by node name. */
public interface GetPersonMembershipsQueryHandler {
  List<PersonMembershipResult> handle(GetPersonMembershipsQuery query);
}
