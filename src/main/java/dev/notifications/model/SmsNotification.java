package dev.notifications.model;

/**
 * Notificación por SMS. Requiere destinatario (número telefónico) y mensaje.
 * No requiere asunto.
 */
public record SmsNotification(
        String recipient,
        String message
) implements Notification {
}
