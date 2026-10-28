package dev.notifications.api;

import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;

/**
 * API pública de la librería de notificaciones.
 *
 * <p>El cliente invoca una sola operación pública para enviar cualquier tipo de notificación,
 * sin depender de clases específicas de proveedores o canales.
 */
public interface NotificationClient {

    NotificationResult send(Notification notification);
}
