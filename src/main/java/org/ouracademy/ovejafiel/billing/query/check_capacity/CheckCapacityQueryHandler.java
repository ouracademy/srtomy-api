package org.ouracademy.ovejafiel.billing.query.check_capacity;

public interface CheckCapacityQueryHandler {
  /**
   * @throws org.ouracademy.ovejafiel.shared.domain.PlanLimitException when the free plan is full
   */
  CheckCapacityResult handle(CheckCapacityQuery query);
}
