package dev.notifications.adapter.push;

import dev.notifications.model.NotificationResult;
import dev.notifications.model.PushNotification;
import dev.notifications.port.NotificationSender;

/**
 * Adaptador simulado para notificaciones Push.
 *
 * <p>No realiza conexiones de red ni usa SDK de proveedores externos.
 * Devuelve un resultado {@code SENT} determinista con un identificador simulado
 * derivado del destinatario. No mantiene estado mutable ni modifica la notificación recibida.
 */
public class SimulatedPushSender implements NotificationSender<PushNotification> {

    @Override
    public Class<PushNotification> notificationType() {
        return PushNotification.class;
    }

    @Override
    public NotificationResult send(PushNotification notification) {
        return NotificationResult.sent("sim-push-" + notification.recipient());
    }
}
