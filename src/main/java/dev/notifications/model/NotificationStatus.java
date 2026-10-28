package dev.notifications.model;

/**
 * Estado de una notificación tras el intento de envío.
 *
 * <p>{@code SENT} indica que el adaptador/proveedor aceptó el mensaje.
 * No garantiza entrega ni lectura por el destinatario.
 * {@code FAILED} indica que el envío no se pudo completar.
 */
public enum NotificationStatus {
    SENT,
    FAILED
}
