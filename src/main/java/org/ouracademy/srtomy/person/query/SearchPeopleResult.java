package org.ouracademy.srtomy.person.query;

import org.springframework.data.domain.Page;

public record SearchPeopleResult(Page<FindPersonResult> people) {}
