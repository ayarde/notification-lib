package dev.notifications.model;

/**
 * Contrato común para todas las notificaciones, independientemente del canal.
 *
 * <p>{@code recipient} representa la dirección de destino según el canal:
 * dirección de email, número telefónico o token/destino de dispositivo Push.
 */
public interface Notification {

    String recipient();
}
