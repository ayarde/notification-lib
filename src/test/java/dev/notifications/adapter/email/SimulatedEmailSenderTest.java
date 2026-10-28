package dev.notifications.adapter.email;

import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link SimulatedEmailSender}.
 */
class SimulatedEmailSenderTest {

    @Test
    void notificationTypeDevuelveEmailNotification() {
        SimulatedEmailSender sender = new SimulatedEmailSender();

        assertEquals(EmailNotification.class, sender.notificationType());
    }

    @Test
    void sendDevuelveResultadoSentConIdSimulado() {
        SimulatedEmailSender sender = new SimulatedEmailSender();
        EmailNotification email = new EmailNotification("usuario@example.com", "Asunto", "Cuerpo");

        NotificationResult resultado = sender.send(email);

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals(NotificationStatus.SENT, resultado.status());
        assertEquals("sim-email-usuario@example.com", resultado.providerMessageId());
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void sendEsDeterministaMismoIdParaMismoDestinatario() {
        SimulatedEmailSender sender = new SimulatedEmailSender();
        EmailNotification email1 = new EmailNotification("a@b.c", "Asunto 1", "Cuerpo 1");
        EmailNotification email2 = new EmailNotification("a@b.c", "Asunto 2", "Cuerpo 2");

        NotificationResult r1 = sender.send(email1);
        NotificationResult r2 = sender.send(email2);

        assertEquals(r1.providerMessageId(), r2.providerMessageId());
    }

    @Test
    void sendNoModificaNotificacionEntrante() {
        SimulatedEmailSender sender = new SimulatedEmailSender();
        EmailNotification email = new EmailNotification("usuario@example.com", "Asunto", "Cuerpo");

        sender.send(email);

        assertEquals("usuario@example.com", email.recipient());
        assertEquals("Asunto", email.subject());
        assertEquals("Cuerpo", email.body());
    }
}
