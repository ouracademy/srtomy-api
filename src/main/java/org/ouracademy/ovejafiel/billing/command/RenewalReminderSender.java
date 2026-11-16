package org.ouracademy.ovejafiel.billing.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.RenewalReminder;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQuery;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.ovejafiel.iam.query.list_access.ListAccessQuery;
import org.ouracademy.ovejafiel.iam.query.list_access.ListAccessQueryHandler;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.query.find_user_by_id.FindUserByIdQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_id.FindUserByIdQueryHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emails one renewal reminder of a church to everyone who manages its plan ({@code
 * billing.manage}), through the outbox, and marks it as sent in the same transaction.
 */
@Component
@RequiredArgsConstructor
public class RenewalReminderSender {

  private static final DateTimeFormatter DAY =
      DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es"));

  private final SubscriptionRepository subscriptions;
  private final ListAccessQueryHandler listAccess;
  private final FindUserByIdQueryHandler findUser;
  private final GetEffectivePermissionsQueryHandler effectivePermissions;
  private final FindOrganizationByIdQueryHandler findOrganization;
  private final SendEmailCommandHandler sendEmail;
  private final BillingPolicy policy;

  @Value("${app.url:http://localhost:3000}")
  private String appUrl;

  /**
   * @return the emails queued, or -1 when this reminder had already been sent
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int remind(PartyId churchId, LocalDate paidUntil, RenewalReminder kind) {
    if (subscriptions.markReminded(churchId.value(), paidUntil, kind.name()) == 0) {
      return -1;
    }
    var name =
        findOrganization
            .handle(new FindOrganizationByIdQuery(churchId))
            .map(o -> o.name())
            .orElse("tu iglesia");
    var subject = subject(kind, name);
    var text = text(kind, name, paidUntil, appUrl + "/portal/" + churchId.value() + "/billing");
    var emails = new LinkedHashSet<String>();
    for (var access : listAccess.handle(new ListAccessQuery(churchId))) {
      if (access.email() == null || access.email().isBlank() || emails.contains(access.email())) {
        continue;
      }
      var user = findUser.handle(new FindUserByIdQuery(access.userId()));
      if (user.isPresent()
          && effectivePermissions
              .handle(new GetEffectivePermissionsQuery(user.get().subject(), churchId))
              .permissions()
              .contains(Permission.BILLING_MANAGE)) {
        emails.add(access.email());
      }
    }
    for (var email : emails) {
      sendEmail.handle(new SendEmailCommand("BILLING_RENEWAL", email, subject, text));
    }
    return emails.size();
  }

  private static String subject(RenewalReminder kind, String church) {
    return switch (kind) {
      case SEVEN_DAYS -> "El plan Iglesia de " + church + " vence en 7 días";
      case ONE_DAY -> "El plan Iglesia de " + church + " vence mañana";
      case GRACE -> "El plan Iglesia de " + church + " venció: renuévalo para no perder funciones";
    };
  }

  private String text(RenewalReminder kind, String church, LocalDate paidUntil, String link) {
    var day = DAY.format(paidUntil);
    var opening =
        switch (kind) {
          case SEVEN_DAYS, ONE_DAY ->
              "El plan Iglesia de " + church + " está pagado hasta el " + day + ".";
          case GRACE ->
              "El plan Iglesia de "
                  + church
                  + " venció el "
                  + day
                  + ". Tienes "
                  + policy.graceDays()
                  + " días para renovarlo antes de volver a los límites del plan Gratis.";
        };
    return opening
        + "\n\nEl pago no se renueva solo: puedes pagar un mes o un año con tarjeta, Yape o PSE"
        + " aquí:\n"
        + link
        + "\n\nNo se borra nada de lo que tu iglesia ya tiene.\n\nSr. Tommy";
  }
}
