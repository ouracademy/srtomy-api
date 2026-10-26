package org.ouracademy.srtomy.outbox.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, OutboxMessageId> {

  /**
   * The next message that is due, locked so that two instances draining at once never take the same
   * one (the other skips it).
   */
  @Query(
      value =
          """
          select * from outbox_message
          where status = 'PENDING' and next_attempt_at <= :now
          order by next_attempt_at, id
          limit 1
          for update skip locked
          """,
      nativeQuery = true)
  Optional<OutboxMessage> lockNextDue(LocalDateTime now);

  /**
   * A projection, not entities: the drain updates messages in its own transaction and session, so
   * an entity cached by the request that enqueued it would still say PENDING.
   */
  @Query(
      "select new org.ouracademy.srtomy.outbox.repository.OutboxMessageStatus(m.id, m.status)"
          + " from OutboxMessage m where m.id in :ids")
  List<OutboxMessageStatus> findStatusesByIdIn(Collection<OutboxMessageId> ids);
}
