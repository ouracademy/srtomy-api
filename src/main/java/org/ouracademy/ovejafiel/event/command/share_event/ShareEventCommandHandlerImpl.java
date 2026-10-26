package org.ouracademy.ovejafiel.event.command.share_event;

import jakarta.persistence.EntityManager;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.EventShare;
import org.ouracademy.ovejafiel.event.query.EventSharingResult;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventShareRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ShareEventCommandHandlerImpl implements ShareEventCommandHandler {

  private final EventRepository eventRepository;
  private final EventShareRepository eventShareRepository;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final SendEmailCommandHandler sendEmailCommandHandler;
  private final EntityManager entityManager;

  @Value("${app.url:http://localhost:3000}")
  private String appUrl;

  @Override
  @Transactional
  public ShareEventResult handle(ShareEventCommand command) {
    var email = normalized(command.email());
    var event =
        eventRepository
            .findById(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    var sharer =
        findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery(command.subject()));

    var share =
        eventShareRepository
            .findByEventIdAndEmailIgnoreCase(event.getId(), email)
            .map(
                existing -> {
                  existing.setRole(command.role());
                  return existing;
                })
            .orElseGet(
                () ->
                    new EventShare(
                        event,
                        email,
                        command.role(),
                        sharer
                            .map(user -> entityManager.getReference(AppUser.class, user.id()))
                            .orElse(null)));
    share = eventShareRepository.save(share);

    var who = sharer.map(user -> user.email()).orElse("Alguien");
    var sent =
        sendEmailCommandHandler
            .handle(
                new SendEmailCommand(
                    email,
                    who + " compartió contigo «" + event.getTitle() + "»",
                    "Hola,\n\n"
                        + who
                        + " compartió contigo «"
                        + event.getTitle()
                        + "» en Sr. Tommy.\n\nÁbrelo aquí (inicia sesión con este correo):\n"
                        + appUrl.replaceAll("/+$", "")
                        + "/e/"
                        + event.getShareSlug()
                        + "\n"))
            .delivered();
    return new ShareEventResult(EventSharingResult.Share.from(share), sent);
  }

  private static String normalized(String email) {
    if (email == null || !email.contains("@")) {
      throw new DomainValidationException("El correo no es válido");
    }
    return email.trim().toLowerCase();
  }
}
