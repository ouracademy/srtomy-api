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
      throw new PlanLimitException(
          "feature." + feature.name().toLowerCase(),
          feature == Feature.WHATSAPP
              ? "La prueba gratuita de WhatsApp terminó. Pasa al plan Iglesia para seguir usándolo."
              : feature.label() + " es parte del plan Iglesia.");
    }
    return new CheckFeatureResult(feature);
  }
}
