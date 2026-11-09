package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import java.util.List;
import org.ouracademy.ovejafiel.whatsapp.command.provision_templates.ProvisionTemplatesResult;

public record ProvisionTemplatesResponse(List<Template> templates) {

  public record Template(String name, boolean submitted, String detail) {}

  public static ProvisionTemplatesResponse from(ProvisionTemplatesResult result) {
    return new ProvisionTemplatesResponse(
        result.templates().stream()
            .map(t -> new Template(t.name(), t.submitted(), t.detail()))
            .toList());
  }
}
