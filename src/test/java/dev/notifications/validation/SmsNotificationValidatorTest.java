package dev.notifications.validation;

import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.SmsNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del validador de SMS: casos válidos e inválidos.
 */
class SmsNotificationValidatorTest {

    private final SmsNotificationValidator validator = new SmsNotificationValidator();

    @Test
    void smsValidoNoLanzaExcepcion() {
        SmsNotification sms = new SmsNotification("+5491112345678", "Hola, prueba de SMS");
        validator.validate(sms); // no lanza
    }

    @Test
    void destinatarioVacioLanzaExcepcion() {
        SmsNotification sms = new SmsNotification("", "Mensaje");
        assertThrows(NotificationValidationException.class, () -> validator.validate(sms));
    }

    @Test
    void mensajeVacioLanzaExcepcion() {
        SmsNotification sms = new SmsNotification("+5491112345678", "");
        assertThrows(NotificationValidationException.class, () -> validator.validate(sms));
    }

    @Test
    void mensajeSoloEspaciosLanzaExcepcion() {
        SmsNotification sms = new SmsNotification("+5491112345678", "   ");
        assertThrows(NotificationValidationException.class, () -> validator.validate(sms));
    }

    @Test
    void destinatarioConLetrasLanzaExcepcion() {
        SmsNotification sms = new SmsNotification("abc123", "Mensaje");
        assertThrows(NotificationValidationException.class, () -> validator.validate(sms));
    }

    @Test
    void destinatarioConCaracteresEspecialesLanzaExcepcion() {
        SmsNotification sms = new SmsNotification("+54-911-1234", "Mensaje");
        assertThrows(NotificationValidationException.class, () -> validator.validate(sms));
    }
}
