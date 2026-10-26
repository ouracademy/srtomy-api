package org.ouracademy.srtomy.outbox.query.find_outbox_statuses;

import java.util.Collection;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

public record FindOutboxStatusesQuery(Collection<OutboxMessageId> ids) {}
