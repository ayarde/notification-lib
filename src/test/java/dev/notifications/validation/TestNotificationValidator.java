package dev.notifications.validation;

import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.Notification;
import dev.notifications.model.TestNotification;

/**
 * Validador de prueba para TestNotification (ámbito de test únicamente).
 *
 * <p>Valida que el destinatario no esté vacío ni en blanco. No forma parte de la API
 * pública de producción; existe únicamente para verificar que un validador nuevo puede
 * registrarse en DefaultNotificationValidator sin modificar el servicio de aplicación.
 */
public class TestNotificationValidator implements NotificationValidator {

    @Override
    public Class<? extends Notification> forType() {
        return TestNotification.class;
    }

    @Override
    public void validate(Notification notification) {
        if (!(notification instanceof TestNotification test)) {
            throw new IllegalArgumentException(
                    "TestNotificationValidator solo valida TestNotification, recibió: "
                            + notification.getClass().getName());
        }

        if (test.recipient() == null || test.recipient().isBlank()) {
            throw new NotificationValidationException(
                    "El destinatario de la notificación de prueba es obligatorio");
        }
    }
}
