package dev.notifications.model;

/**
 * Notificación por Email. Requiere destinatario, asunto y cuerpo.
 */
public record EmailNotification(
        String recipient,
        String subject,
        String body
) implements Notification {
}
