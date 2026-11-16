package org.ouracademy.ovejafiel.billing.query.check_feature;

public interface CheckFeatureQueryHandler {
  /**
   * @throws org.ouracademy.ovejafiel.shared.domain.PlanLimitException when the plan does not
   *     include the feature
   */
  CheckFeatureResult handle(CheckFeatureQuery query);
}
