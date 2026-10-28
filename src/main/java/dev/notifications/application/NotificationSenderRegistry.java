package dev.notifications.application;

import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.port.NotificationSender;

/**
 * Registro interno de adaptadores de envío.
 *
 * <p>Admite múltiples proveedores por tipo de notificación, identificados cada uno mediante
 * un {@code providerId} único. Mantiene un proveedor predeterminado por tipo de notificación
 * para el despacho cuando el cliente no especifica un proveedor concreto.
 *
 * <p>Resuelve la diferencia entre el tipo común {@link Notification} y el tipo concreto que
 * cada adaptador acepta, encapsulando el despacho y evitando casts genéricos inseguros.
 */
public interface NotificationSenderRegistry {

    /**
     * Registra un proveedor de envío para un tipo de notificación.
     *
     * @param providerId identificador único del proveedor
     * @param type       tipo de notificación que el proveedor acepta
     * @param sender     adaptador a registrar
     * @throws NotificationConfigurationException si el providerId ya existe o el proveedor no es
     *                                            compatible con el type declarado
     */
    void register(String providerId, Class<? extends Notification> type, NotificationSender<?> sender);

    /**
     * Define el proveedor predeterminado para un tipo de notificación.
     *
     * @param type        tipo de notificación
     * @param providerId  identificador único del proveedor ya registrado
     * @throws NotificationConfigurationException si el providerId no existe o no es compatible
     *                                            con el type
     */
    void setDefaultProvider(Class<? extends Notification> type, String providerId);

    NotificationResult send(Notification notification);

    boolean hasProvider(String providerId);

    boolean hasDefaultProvider(Class<? extends Notification> type);

    String getDefaultProvider(Class<? extends Notification> type);
}
