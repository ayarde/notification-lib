package dev.notifications.validation;

import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.EmailNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del validador de Email: casos válidos e inválidos.
 */
class EmailNotificationValidatorTest {

    private final EmailNotificationValidator validator = new EmailNotificationValidator();

    @Test
    void emailValidoNoLanzaExcepcion() {
        EmailNotification email = new EmailNotification("usuario@example.com", "Asunto", "Cuerpo");
        validator.validate(email); // no lanza
    }

    @Test
    void destinatarioVacioLanzaExcepcion() {
        EmailNotification email = new EmailNotification("", "Asunto", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }

    @Test
    void destinatarioEnBlancoLanzaExcepcion() {
        EmailNotification email = new EmailNotification("   ", "Asunto", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }

    @Test
    void destinatarioSinArrobaLanzaExcepcion() {
        EmailNotification email = new EmailNotification("usuarioexample.com", "Asunto", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }

    @Test
    void destinatarioFormatoIncompletoUsuarioArrobaLanzaExcepcion() {
        EmailNotification email = new EmailNotification("usuario@", "Asunto", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }

    @Test
    void asuntoVacioLanzaExcepcion() {
        EmailNotification email = new EmailNotification("a@b.c", "", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }

    @Test
    void asuntoSoloEspaciosLanzaExcepcion() {
        EmailNotification email = new EmailNotification("a@b.c", "   ", "Cuerpo");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }

    @Test
    void cuerpoVacioLanzaExcepcion() {
        EmailNotification email = new EmailNotification("a@b.c", "Asunto", "");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }

    @Test
    void cuerpoSoloEspaciosLanzaExcepcion() {
        EmailNotification email = new EmailNotification("a@b.c", "Asunto", "   ");
        assertThrows(NotificationValidationException.class, () -> validator.validate(email));
    }
}
