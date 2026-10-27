package org.ouracademy.srtomy.outbox.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, OutboxMessageId> {

  /** Locked, so two calls for the same message (the queue may repeat one) take turns. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from OutboxMessage m where m.id = :id")
  Optional<OutboxMessage> findForUpdate(OutboxMessageId id);

  /** Messages still waiting that nothing has touched since {@code before}. */
  @Query(
      "select m from OutboxMessage m where m.status = org.ouracademy.srtomy.outbox.domain.OutboxStatus.PENDING"
          + " and m.lastModifiedDate < :before order by m.lastModifiedDate")
  List<OutboxMessage> findPendingUntouchedSince(LocalDateTime before, Pageable pageable);

  /**
   * A projection, not entities: the attempt updates messages in its own transaction and session, so
   * an entity cached by the request that enqueued it would still say PENDING.
   */
  @Query(
      "select new org.ouracademy.srtomy.outbox.repository.OutboxMessageStatus(m.id, m.status)"
          + " from OutboxMessage m where m.id in :ids")
  List<OutboxMessageStatus> findStatusesByIdIn(Collection<OutboxMessageId> ids);
}
