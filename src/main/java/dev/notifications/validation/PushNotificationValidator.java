package dev.notifications.validation;

import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.Notification;
import dev.notifications.model.PushNotification;

/**
 * Validador de notificaciones Push.
 *
 * <p>Valida que destino de dispositivo, título y cuerpo no estén vacíos ni en blanco.
 */
public class PushNotificationValidator implements NotificationValidator {

    @Override
    public Class<? extends Notification> forType() {
        return PushNotification.class;
    }

    @Override
    public void validate(Notification notification) {
        if (!(notification instanceof PushNotification push)) {
            throw new IllegalArgumentException(
                    "PushNotificationValidator solo valida PushNotification, recibió: "
                            + notification.getClass().getName());
        }

        if (push.recipient() == null || push.recipient().isBlank()) {
            throw new NotificationValidationException("El destino del dispositivo Push es obligatorio");
        }
        if (push.title() == null || push.title().isBlank()) {
            throw new NotificationValidationException("El título de la notificación Push es obligatorio");
        }
        if (push.body() == null || push.body().isBlank()) {
            throw new NotificationValidationException("El cuerpo de la notificación Push es obligatorio");
        }
    }
}
