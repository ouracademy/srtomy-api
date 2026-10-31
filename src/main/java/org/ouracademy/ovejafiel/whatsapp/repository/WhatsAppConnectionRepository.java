package org.ouracademy.ovejafiel.whatsapp.repository;

import java.util.Optional;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppConnectionRepository extends JpaRepository<WhatsAppConnection, PartyId> {

  Optional<WhatsAppConnection> findByKapsoCustomerId(String kapsoCustomerId);
}
