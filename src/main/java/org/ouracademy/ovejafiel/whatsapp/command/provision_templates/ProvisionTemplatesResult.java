package org.ouracademy.ovejafiel.whatsapp.command.provision_templates;

import java.util.List;

public record ProvisionTemplatesResult(List<Outcome> templates) {

  /**
   * @param submitted true when Meta received it for review
   * @param detail why not, when it was not (for instance "already exists")
   */
  public record Outcome(String name, boolean submitted, String detail) {}
}
