package org.ouracademy.ovejafiel.billing.query.check_capacity;

/**
 * @param max null when the plan has no maximum
 */
public record CheckCapacityResult(long used, Integer max) {}
