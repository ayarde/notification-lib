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
    void sendDevuelveResultadoSentConIdSimulado() {
        SimulatedSmsSender sender = new SimulatedSmsSender();
        SmsNotification sms = new SmsNotification("+5491112345678", "Mensaje de prueba");

        NotificationResult resultado = sender.send(sms);

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals(NotificationStatus.SENT, resultado.status());
        assertEquals("sim-sms-+5491112345678", resultado.providerMessageId());
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void sendEsDeterministaMismoIdParaMismoDestinatario() {
        SimulatedSmsSender sender = new SimulatedSmsSender();
        SmsNotification sms1 = new SmsNotification("+12345", "Mensaje 1");
        SmsNotification sms2 = new SmsNotification("+12345", "Mensaje 2");

        NotificationResult r1 = sender.send(sms1);
        NotificationResult r2 = sender.send(sms2);

        assertEquals(r1.providerMessageId(), r2.providerMessageId());
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
