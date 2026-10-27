package org.ouracademy.ovejafiel.invitation.repository;

import java.util.List;
import java.util.Optional;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationId;
import org.ouracademy.ovejafiel.invitation.domain.InvitationStatus;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, InvitationId> {

  Optional<Invitation> findByTokenHash(String tokenHash);

  List<Invitation> findByOrganizationIdAndStatusOrderByCreateDateDesc(
      PartyId organizationId, InvitationStatus status);

  List<Invitation> findByEmailIgnoreCaseAndStatusOrderByCreateDateDesc(
      String email, InvitationStatus status);
}
