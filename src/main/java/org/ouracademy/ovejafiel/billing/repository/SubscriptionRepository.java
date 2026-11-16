package org.ouracademy.ovejafiel.billing.repository;

import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, PartyId> {}
