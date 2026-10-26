package org.ouracademy.ovejafiel.person.query.search_people;

import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.springframework.data.domain.Page;

public record SearchPeopleResult(Page<FindPersonResult> people) {}
