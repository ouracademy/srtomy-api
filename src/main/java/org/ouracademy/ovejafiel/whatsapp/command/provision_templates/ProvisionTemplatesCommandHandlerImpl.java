package org.ouracademy.ovejafiel.whatsapp.command.provision_templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.whatsapp.command.provision_templates.ProvisionTemplatesResult.Outcome;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Service;

/** Not transactional: it only reads, and each call to the provider must not hold the database. */
@Service
@RequiredArgsConstructor
class ProvisionTemplatesCommandHandlerImpl implements ProvisionTemplatesCommandHandler {

  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final WhatsAppConnectionRepository connections;
  private final WhatsAppGateway gateway;

  @Override
  public ProvisionTemplatesResult handle(ProvisionTemplatesCommand command) {
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var connection = connections.findById(churchId).filter(c -> c.isConnected()).orElse(null);
    if (connection == null) {
      throw new DomainValidationException("La iglesia todavía no conectó su WhatsApp");
    }
    var account = connection.getBusinessAccountId();
    if (account == null || account.isBlank()) {
      throw new DomainValidationException(
          "No se conoce la cuenta de WhatsApp Business de la iglesia: vuelve a conectar el número");
    }

    var outcomes = new ArrayList<Outcome>();
    for (var template :
        Arrays.stream(WhatsAppTemplate.values()).filter(WhatsAppTemplate::isProvisioned).toList()) {
      outcomes.add(submit(account, template));
    }
    return new ProvisionTemplatesResult(outcomes);
  }

  private Outcome submit(String account, WhatsAppTemplate template) {
    try {
      gateway.createTemplate(account, template);
      return new Outcome(template.metaName(), true, null);
    } catch (RuntimeException e) {
      // One template refused (it exists already, or Meta did not like it) does not stop the rest.
      return new Outcome(template.metaName(), false, e.getMessage());
    }
  }
}
