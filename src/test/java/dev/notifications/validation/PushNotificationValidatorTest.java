package dev.notifications.validation;

import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.PushNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del validador de Push: casos válidos e inválidos.
 */
class PushNotificationValidatorTest {

    private final PushNotificationValidator validator = new PushNotificationValidator();

    @Test
    void pushValidoNoLanzaExcepcion() {
        PushNotification push = new PushNotification("token-dispositivo-123", "Título", "Cuerpo");
        validator.validate(push); // no lanza
    }

    @Test
    void destinoVacioLanzaExcepcion() {
        PushNotification push = new PushNotification("", "Título", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(push));
    }

    @Test
    void destinoSoloEspaciosLanzaExcepcion() {
        PushNotification push = new PushNotification("   ", "Título", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(push));
    }

    @Test
    void tituloVacioLanzaExcepcion() {
        PushNotification push = new PushNotification("token-123", "", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(push));
    }

    @Test
    void tituloSoloEspaciosLanzaExcepcion() {
        PushNotification push = new PushNotification("token-123", "   ", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(push));
    }

    @Test
    void cuerpoVacioLanzaExcepcion() {
        PushNotification push = new PushNotification("token-123", "Título", "");
        assertThrows(NotificationValidationException.class, () -> validator.validate(push));
    }

    @Test
    void cuerpoSoloEspaciosLanzaExcepcion() {
        PushNotification push = new PushNotification("token-123", "Título", "   ");
        assertThrows(NotificationValidationException.class, () -> validator.validate(push));
    }
}
