package org.ouracademy.ovejafiel.billing.command.start_whatsapp_trial;

import java.time.LocalDateTime;

/**
 * @param trialEndsAt null when the church has the paid plan
 */
public record StartWhatsappTrialResult(LocalDateTime trialEndsAt) {}
