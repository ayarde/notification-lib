package dev.notifications.model;

/**
 * Notificación de prueba para demostrar extensibilidad (ámbito de test únicamente).
 *
 * <p>Implementa {@link Notification} con un campo recipient. No forma parte de la API
 * pública de producción; existe únicamente para verificar que un canal nuevo puede
 * agregarse mediante un modelo y adaptador nuevos sin modificar el servicio de aplicación.
 */
public record TestNotification(
        String recipient
) implements Notification {
}
