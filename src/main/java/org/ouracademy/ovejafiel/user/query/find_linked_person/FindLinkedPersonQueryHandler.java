package org.ouracademy.ovejafiel.user.query.find_linked_person;

import java.util.Optional;

public interface FindLinkedPersonQueryHandler {
  Optional<FindLinkedPersonResult> handle(FindLinkedPersonQuery query);
}
