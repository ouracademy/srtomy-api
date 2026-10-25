package org.ouracademy.srtomy.person.query.find_people_by_email;

public interface FindPeopleByEmailQueryHandler {

  /** The email is compared ignoring case and surrounding spaces; a blank email matches nobody. */
  FindPeopleByEmailResult handle(FindPeopleByEmailQuery query);
}
