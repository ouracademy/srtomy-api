package org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses;

public interface FindOutboxStatusesQueryHandler {
  FindOutboxStatusesResult handle(FindOutboxStatusesQuery query);
}
