package dev.notifications.adapter.push;

import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import dev.notifications.model.PushNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link SimulatedPushSender}.
 */
class SimulatedPushSenderTest {

    @Test
    void notificationTypeDevuelvePushNotification() {
        SimulatedPushSender sender = new SimulatedPushSender();

        assertEquals(PushNotification.class, sender.notificationType());
    }

    @Test
    void sendDevuelveResultadoSentConIdSimuladoOpcio() {
        SimulatedPushSender sender = new SimulatedPushSender();
        PushNotification push = new PushNotification("token-dispositivo-123", "Título", "Cuerpo");

        NotificationResult resultado = sender.send(push);

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals(NotificationStatus.SENT, resultado.status());
        assertNotNull(resultado.providerMessageId());
        assertTrue(resultado.providerMessageId().startsWith("sim-push-"));
        assertFalse(resultado.providerMessageId().contains("token-dispositivo-123"));
        assertFalse(resultado.providerMessageId().contains("token-dispositivo"));
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void sendGeneraIdentificadorOpcioNoDerivadoDelDestinatario() {
        SimulatedPushSender sender = new SimulatedPushSender();
        String destinatario = "token-dispositivo-123";
        PushNotification push = new PushNotification(destinatario, "Título", "Cuerpo");

        NotificationResult resultado = sender.send(push);

        assertNotNull(resultado.providerMessageId());
        assertFalse(resultado.providerMessageId().contains(destinatario));
        assertFalse(resultado.providerMessageId().contains("token-dispositivo"));
    }

    @Test
    void sendGeneraIdsDiferentesParaCadaEnvio() {
        SimulatedPushSender sender = new SimulatedPushSender();
        PushNotification push1 = new PushNotification("tok-1", "Título 1", "Cuerpo 1");
        PushNotification push2 = new PushNotification("tok-1", "Título 2", "Cuerpo 2");

        NotificationResult r1 = sender.send(push1);
        NotificationResult r2 = sender.send(push2);

        assertNotEquals(r1.providerMessageId(), r2.providerMessageId());
        assertTrue(r1.providerMessageId().startsWith("sim-push-"));
        assertTrue(r2.providerMessageId().startsWith("sim-push-"));
    }

    @Test
    void sendNoModificaNotificacionEntrante() {
        SimulatedPushSender sender = new SimulatedPushSender();
        PushNotification push = new PushNotification("token-dispositivo-123", "Título", "Cuerpo");

        sender.send(push);

        assertEquals("token-dispositivo-123", push.recipient());
        assertEquals("Título", push.title());
        assertEquals("Cuerpo", push.body());
    }
}
