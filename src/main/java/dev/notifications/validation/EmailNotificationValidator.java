package dev.notifications.validation;

import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.Notification;

/**
 * Validador de notificaciones Email.
 *
 * <p>Valida que destinatario, asunto y cuerpo no estén vacíos ni en blanco, y que el
 * destinatario tenga un formato razonable de email (contiene @ con dominio). Este formato
 * es preliminar; no garantiza la existencia real de la dirección.
 */
public class EmailNotificationValidator implements NotificationValidator {

    @Override
    public Class<? extends Notification> forType() {
        return EmailNotification.class;
    }

    @Override
    public void validate(Notification notification) {
        if (!(notification instanceof EmailNotification email)) {
            throw new IllegalArgumentException(
                    "EmailNotificationValidator solo valida EmailNotification, recibió: "
                            + notification.getClass().getName());
        }

        if (email.recipient() == null || email.recipient().isBlank()) {
            throw new NotificationValidationException("El destinatario del email es obligatorio");
        }
        int arroba = email.recipient().indexOf('@');
        if (arroba == -1 || arroba == 0 || arroba == email.recipient().length() - 1) {
            throw new NotificationValidationException(
                    "El destinatario del email no tiene un formato razonable");
        }
        if (email.subject() == null || email.subject().isBlank()) {
            throw new NotificationValidationException("El asunto del email es obligatorio");
        }
        if (email.body() == null || email.body().isBlank()) {
            throw new NotificationValidationException("El cuerpo del email es obligatorio");
        }
    }
}
