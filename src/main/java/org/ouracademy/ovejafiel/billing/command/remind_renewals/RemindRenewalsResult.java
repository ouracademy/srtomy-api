package org.ouracademy.ovejafiel.billing.command.remind_renewals;

/**
 * @param churches churches reminded in this run (the ones already reminded are not counted)
 * @param emails emails queued, one per person who manages the plan
 */
public record RemindRenewalsResult(int churches, int emails) {}
