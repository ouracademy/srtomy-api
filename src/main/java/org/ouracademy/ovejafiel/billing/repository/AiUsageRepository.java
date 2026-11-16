package org.ouracademy.ovejafiel.billing.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ouracademy.ovejafiel.billing.domain.AiUsage;
import org.ouracademy.ovejafiel.billing.domain.AiUsageId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * The counters only change with these single statements, so two instances of the agent answering at
 * the same time never go past the limit nor lose tokens.
 */
public interface AiUsageRepository extends JpaRepository<AiUsage, AiUsageId> {

  Optional<AiUsage> findByOrganizationIdAndMonth(PartyId organizationId, LocalDate month);

  /**
   * Adds one message to the month of the church only while it is under both caps; answers the new
   * count, or nothing when a cap was reached. {@code id} is only used if the month has no row yet.
   */
  @Query(
      nativeQuery = true,
      value =
          """
          insert into billing_ai_usage
            (id, organization_id, month, messages, cost_usd, create_date, last_modified_date)
          values (:id, :organizationId, :month, 1, 0, now(), now())
          on conflict (organization_id, month) do update
            set messages = billing_ai_usage.messages + 1, last_modified_date = now()
            where billing_ai_usage.messages < :maxMessages
              and billing_ai_usage.cost_usd < :maxCostUsd
          returning messages
          """)
  List<Integer> reserveMessage(
      UUID id, UUID organizationId, LocalDate month, int maxMessages, BigDecimal maxCostUsd);

  /** Adds the tokens and the estimated cost of one answer to the month of the church. */
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          insert into billing_ai_usage
            (id, organization_id, month, messages, input_tokens, output_tokens, cache_read_tokens,
             cache_write_tokens, cost_usd, create_date, last_modified_date)
          values (:id, :organizationId, :month, 0, :input, :output, :cacheRead, :cacheWrite, :cost,
                  now(), now())
          on conflict (organization_id, month) do update
            set input_tokens = billing_ai_usage.input_tokens + excluded.input_tokens,
                output_tokens = billing_ai_usage.output_tokens + excluded.output_tokens,
                cache_read_tokens = billing_ai_usage.cache_read_tokens + excluded.cache_read_tokens,
                cache_write_tokens = billing_ai_usage.cache_write_tokens + excluded.cache_write_tokens,
                cost_usd = billing_ai_usage.cost_usd + excluded.cost_usd,
                last_modified_date = now()
          """)
  int addTokens(
      UUID id,
      UUID organizationId,
      LocalDate month,
      long input,
      long output,
      long cacheRead,
      long cacheWrite,
      BigDecimal cost);
}
