package org.ouracademy.srtomy.invitation.repository;

import java.util.List;
import java.util.Optional;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationId;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, InvitationId> {

  Optional<Invitation> findByTokenHash(String tokenHash);

  List<Invitation> findByOrganizationIdAndStatusOrderByCreateDateDesc(
      PartyId organizationId, InvitationStatus status);

  List<Invitation> findByEmailIgnoreCaseAndStatusOrderByCreateDateDesc(
      String email, InvitationStatus status);
}
