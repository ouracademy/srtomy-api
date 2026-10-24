package org.ouracademy.srtomy.person.query.search_people;

import org.ouracademy.srtomy.person.query.FindPersonResult;
import org.springframework.data.domain.Page;

/**
 * People of a node and of everything below it. {@code organizationName} says where each one comes
 * from; {@code direct} is true when they belong to the searched node itself.
 */
public record SearchPeopleResult(Page<Item> people) {

  public record Item(FindPersonResult person, String organizationName, boolean direct) {}
}
