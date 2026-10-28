package dev.notifications.adapter;

import dev.notifications.model.NotificationResult;
import dev.notifications.model.TestNotification;
import dev.notifications.port.NotificationSender;

/**
 * Adaptador simulado de prueba para TestNotification (ámbito de test únicamente).
 *
 * <p>No realiza conexiones de red. Devuelve un resultado SENT determinista con un
 * identificador simulado derivado del destinatario. No mantiene estado mutable ni
 * modifica la notificación recibida.
 */
public class TestNotificationSender implements NotificationSender<TestNotification> {

    @Override
    public Class<TestNotification> notificationType() {
        return TestNotification.class;
    }

    @Override
    public NotificationResult send(TestNotification notification) {
        return NotificationResult.sent("sim-test-" + notification.recipient());
    }
}
