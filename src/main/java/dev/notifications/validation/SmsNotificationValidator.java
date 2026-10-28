package dev.notifications.validation;

import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.Notification;
import dev.notifications.model.SmsNotification;

/**
 * Validador de notificaciones SMS.
 *
 * <p>Valida que destinatario y mensaje no estén vacíos ni en blanco, y que el destinatario
 * tenga un formato razonable de número telefónico (dígitos, signo +, espacios). Este formato
 * es preliminar; no garantiza la existencia real del número.
 */
public class SmsNotificationValidator implements NotificationValidator {

    @Override
    public Class<? extends Notification> forType() {
        return SmsNotification.class;
    }

    @Override
    public void validate(Notification notification) {
        if (!(notification instanceof SmsNotification sms)) {
            throw new IllegalArgumentException(
                    "SmsNotificationValidator solo valida SmsNotification, recibió: "
                            + notification.getClass().getName());
        }

        if (sms.recipient() == null || sms.recipient().isBlank()) {
            throw new NotificationValidationException("El destinatario del SMS es obligatorio");
        }
        if (!sms.recipient().matches("[\\d\\+\\s]+")) {
            throw new NotificationValidationException(
                    "El destinatario del SMS no tiene un formato razonable: " + sms.recipient());
        }
        if (sms.message() == null || sms.message().isBlank()) {
            throw new NotificationValidationException("El mensaje del SMS es obligatorio");
        }
    }
}
