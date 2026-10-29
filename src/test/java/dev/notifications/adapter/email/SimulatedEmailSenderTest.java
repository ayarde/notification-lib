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
    void sendDevuelveResultadoSentConIdSimuladoOpcio() {
        SimulatedEmailSender sender = new SimulatedEmailSender();
        EmailNotification email = new EmailNotification("usuario@example.com", "Asunto", "Cuerpo");

        NotificationResult resultado = sender.send(email);

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals(NotificationStatus.SENT, resultado.status());
        assertNotNull(resultado.providerMessageId());
        assertTrue(resultado.providerMessageId().startsWith("sim-email-"));
        assertFalse(resultado.providerMessageId().contains("usuario@example.com"));
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void sendGeneraIdentificadorOpcioNoDerivadoDelDestinatario() {
        SimulatedEmailSender sender = new SimulatedEmailSender();
        String destinatario = "usuario@example.com";
        EmailNotification email = new EmailNotification(destinatario, "Asunto", "Cuerpo");

        NotificationResult resultado = sender.send(email);

        assertNotNull(resultado.providerMessageId());
        assertFalse(resultado.providerMessageId().contains(destinatario));
        assertFalse(resultado.providerMessageId().contains("usuario"));
        assertFalse(resultado.providerMessageId().contains("example.com"));
    }

    @Test
    void sendGeneraIdsDiferentesParaCadaEnvio() {
        SimulatedEmailSender sender = new SimulatedEmailSender();
        EmailNotification email1 = new EmailNotification("a@b.c", "Asunto 1", "Cuerpo 1");
        EmailNotification email2 = new EmailNotification("a@b.c", "Asunto 2", "Cuerpo 2");

        NotificationResult r1 = sender.send(email1);
        NotificationResult r2 = sender.send(email2);

        assertNotEquals(r1.providerMessageId(), r2.providerMessageId());
        assertTrue(r1.providerMessageId().startsWith("sim-email-"));
        assertTrue(r2.providerMessageId().startsWith("sim-email-"));
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
