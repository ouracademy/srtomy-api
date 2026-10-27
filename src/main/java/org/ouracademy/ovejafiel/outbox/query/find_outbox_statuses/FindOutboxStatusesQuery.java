package org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses;

import java.util.Collection;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

public record FindOutboxStatusesQuery(Collection<OutboxMessageId> ids) {}
