package dev.notifications.application;

import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import dev.notifications.port.NotificationSender;

/**
 * Remitente simulado para pruebas de la Etapa 3. No realiza conexiones de red.
 */
public class FixedResultNotificationSender implements NotificationSender<Notification> {

    private final NotificationResult result;

    public FixedResultNotificationSender(NotificationResult result) {
        this.result = result;
    }

    @Override
    public Class<Notification> notificationType() {
        return Notification.class;
    }

    @Override
    public NotificationResult send(Notification notification) {
        return result;
    }
}
