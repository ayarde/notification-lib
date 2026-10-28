package dev.notifications.model;

/**
 * Notificación Push. Requiere destino de dispositivo, título y cuerpo.
 */
public record PushNotification(
        String recipient,
        String title,
        String body
) implements Notification {
}
