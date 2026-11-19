package org.ouracademy.ovejafiel.billing.query.check_feature;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.shared.domain.PlanLimitException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CheckFeatureQueryHandlerImpl implements CheckFeatureQueryHandler {

  private final Entitlements entitlements;

  @Override
  @Transactional(readOnly = true)
  public CheckFeatureResult handle(CheckFeatureQuery query) {
    var feature = query.feature();
    if (!entitlements.of(query.organizationId()).allows(feature)) {
      throw new PlanLimitException("feature." + feature.name().toLowerCase(), message(feature));
    }
    return new CheckFeatureResult(feature);
  }

  private static String message(Feature feature) {
    if (feature == Feature.OWN_NUMBER) {
      return "Conectar un número de WhatsApp propio es parte del plan Red, o un extra de los planes"
          + " Básico e Iglesia. Mientras tanto usas el número de Sr. Tommy.";
    }
    return feature.label() + " viene desde el plan " + feature.cheapestPlan().label() + ".";
  }
}
