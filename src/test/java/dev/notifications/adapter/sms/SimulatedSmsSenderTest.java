package dev.notifications.adapter.sms;

import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import dev.notifications.model.SmsNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link SimulatedSmsSender}.
 */
class SimulatedSmsSenderTest {

    @Test
    void notificationTypeDevuelveSmsNotification() {
        SimulatedSmsSender sender = new SimulatedSmsSender();

        assertEquals(SmsNotification.class, sender.notificationType());
    }

    @Test
    void sendDevuelveResultadoSentConIdSimuladoOpcio() {
        SimulatedSmsSender sender = new SimulatedSmsSender();
        SmsNotification sms = new SmsNotification("+5491112345678", "Mensaje de prueba");

        NotificationResult resultado = sender.send(sms);

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals(NotificationStatus.SENT, resultado.status());
        assertNotNull(resultado.providerMessageId());
        assertTrue(resultado.providerMessageId().startsWith("sim-sms-"));
        assertFalse(resultado.providerMessageId().contains("+5491112345678"));
        assertFalse(resultado.providerMessageId().contains("5491112345678"));
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void sendGeneraIdentificadorOpcioNoDerivadoDelDestinatario() {
        SimulatedSmsSender sender = new SimulatedSmsSender();
        String destinatario = "+5491112345678";
        SmsNotification sms = new SmsNotification(destinatario, "Mensaje de prueba");

        NotificationResult resultado = sender.send(sms);

        assertNotNull(resultado.providerMessageId());
        assertFalse(resultado.providerMessageId().contains(destinatario));
        assertFalse(resultado.providerMessageId().contains("5491112345678"));
    }

    @Test
    void sendGeneraIdsDiferentesParaCadaEnvio() {
        SimulatedSmsSender sender = new SimulatedSmsSender();
        SmsNotification sms1 = new SmsNotification("+12345", "Mensaje 1");
        SmsNotification sms2 = new SmsNotification("+12345", "Mensaje 2");

        NotificationResult r1 = sender.send(sms1);
        NotificationResult r2 = sender.send(sms2);

        assertNotEquals(r1.providerMessageId(), r2.providerMessageId());
        assertTrue(r1.providerMessageId().startsWith("sim-sms-"));
        assertTrue(r2.providerMessageId().startsWith("sim-sms-"));
    }

    @Test
    void sendNoModificaNotificacionEntrante() {
        SimulatedSmsSender sender = new SimulatedSmsSender();
        SmsNotification sms = new SmsNotification("+5491112345678", "Mensaje de prueba");

        sender.send(sms);

        assertEquals("+5491112345678", sms.recipient());
        assertEquals("Mensaje de prueba", sms.message());
    }
}
