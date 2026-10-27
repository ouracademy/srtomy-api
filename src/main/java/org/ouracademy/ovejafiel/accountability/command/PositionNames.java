package org.ouracademy.ovejafiel.accountability.command;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType.Category;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Component;

/**
 * The only rules a position name has: not blank, and unique within its church ignoring case. Any
 * wording is allowed so each tradition can name its servants its own way.
 */
@Component
@RequiredArgsConstructor
public class PositionNames {

  private final AccountabilityTypeRepository accountabilityTypeRepository;

  /** Returns the trimmed name, or fails when it is blank or already used in the church. */
  public String requireAvailable(PartyId churchId, String name) {
    var trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException("El nombre del cargo es obligatorio");
    }
    if (accountabilityTypeRepository.existsByPartyIdAndCategoryAndNameIgnoreCase(
        churchId, Category.POSITION, trimmed)) {
      throw new DomainValidationException("Ya existe un cargo llamado \"" + trimmed + "\"");
    }
    return trimmed;
  }
}
