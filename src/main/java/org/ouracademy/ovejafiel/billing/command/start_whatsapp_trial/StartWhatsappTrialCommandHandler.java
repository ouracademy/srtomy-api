package org.ouracademy.ovejafiel.billing.command.start_whatsapp_trial;

/**
 * On the free plan, connecting WhatsApp starts its trial, once. With the paid plan nothing happens.
 */
public interface StartWhatsappTrialCommandHandler {
  /**
   * @throws org.ouracademy.ovejafiel.shared.domain.PlanLimitException when the trial already ended
   */
  StartWhatsappTrialResult handle(StartWhatsappTrialCommand command);
}
