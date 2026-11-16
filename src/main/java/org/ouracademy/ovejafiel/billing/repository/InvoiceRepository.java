package org.ouracademy.ovejafiel.billing.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.ouracademy.ovejafiel.billing.domain.Invoice;
import org.ouracademy.ovejafiel.billing.domain.InvoiceId;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<Invoice, InvoiceId> {

  List<Invoice> findByOrganizationIdOrderByCreateDateDesc(PartyId organizationId);

  List<Invoice> findByOrganizationIdAndStatus(PartyId organizationId, InvoiceStatus status);

  /** Locked, so a webhook and a manual check of the same payment do not both extend the plan. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Invoice i where i.id = :id")
  Optional<Invoice> findForUpdate(InvoiceId id);

  Optional<Invoice> findByGatewayPaymentId(String gatewayPaymentId);
}
