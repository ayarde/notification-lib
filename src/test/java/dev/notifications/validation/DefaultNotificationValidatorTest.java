package dev.notifications.validation;

import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import dev.notifications.port.NotificationSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del validador predeterminado: registro, delegación por tipo y ausencia de validador.
 */
class DefaultNotificationValidatorTest {

    private NotificationValidator validadorParaTipo(Class<? extends Notification> tipo) {
        return new NotificationValidator() {
            @Override
            public Class<? extends Notification> forType() {
                return tipo;
            }

            @Override
            public void validate(Notification notification) {
                // validador de prueba sin lógica
            }
        };
    }

    @Test
    void registraYValidaDelegandoEnElValidadorDelTipo() {
        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        EmailNotification email = new EmailNotification("a@b.c", "Asunto", "Cuerpo");

        validator.register(new EmailNotificationValidator());
        validator.validate(email); // no lanza

        assertTrue(validator.hasValidatorFor(EmailNotification.class));
    }

    @Test
    void ausenciaDeValidadorLanzaExcepcionDeConfiguracion() {
        DefaultNotificationValidator validator = new DefaultNotificationValidator();

        assertThrows(NotificationConfigurationException.class,
                () -> validator.validate(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));
    }

    @Test
    void registroDuplicadoLanzaExcepcionDeConfiguracion() {
        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        assertThrows(NotificationConfigurationException.class,
                () -> validator.register(new EmailNotificationValidator()));
    }
}
