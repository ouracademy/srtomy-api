package org.ouracademy.ovejafiel.person.query.find_people_by_phone;

public interface FindPeopleByPhoneQueryHandler {

  /** A phone with fewer than 8 digits matches nobody. */
  FindPeopleByPhoneResult handle(FindPeopleByPhoneQuery query);
}
