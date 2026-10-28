package dev.notifications.application;

import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.port.NotificationSender;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementación predeterminada del registro de adaptadores.
 *
 * <p>Mantiene un mapa de proveedores keyed por {@code providerId}, cada uno asociado al tipo de
 * notificación que acepta. Mantiene un segundo mapa que asocia cada tipo de notificación con su
 * proveedor predeterminado. El despacho resuelve el proveedor predeterminado según el tipo de la
 * notificación y verifica la compatibilidad de tipo antes de hacer el cast.
 */
public class DefaultNotificationSenderRegistry implements NotificationSenderRegistry {

    private static class ProviderEntry {
        final Class<? extends Notification> type;
        final NotificationSender<?> sender;

        ProviderEntry(Class<? extends Notification> type, NotificationSender<?> sender) {
            this.type = type;
            this.sender = sender;
        }
    }

    private final Map<String, ProviderEntry> providersById = new HashMap<>();
    private final Map<Class<? extends Notification>, String> defaultProviderByType = new HashMap<>();

    @Override
    public void register(String providerId, Class<? extends Notification> type,
                         NotificationSender<?> sender) {
        Objects.requireNonNull(providerId, "providerId no puede ser nulo");
        Objects.requireNonNull(type, "type no puede ser nulo");
        Objects.requireNonNull(sender, "sender no puede ser nulo");

        if (providersById.containsKey(providerId)) {
            throw new NotificationConfigurationException(
                    "Ya existe un proveedor registrado con el identificador: " + providerId);
        }

        if (!sender.notificationType().isAssignableFrom(type)) {
            throw new NotificationConfigurationException(
                    "El proveedor " + providerId + " no acepta notificaciones del tipo "
                            + type.getName() + "; acepta: "
                            + sender.notificationType().getName());
        }

        providersById.put(providerId, new ProviderEntry(type, sender));
    }

    @Override
    public void setDefaultProvider(Class<? extends Notification> type, String providerId) {
        Objects.requireNonNull(type, "type no puede ser nulo");
        Objects.requireNonNull(providerId, "providerId no puede ser nulo");

        ProviderEntry entry = providersById.get(providerId);
        if (entry == null) {
            throw new NotificationConfigurationException(
                    "No existe un proveedor registrado con el identificador: " + providerId);
        }

        if (!entry.type.isAssignableFrom(type)) {
            throw new NotificationConfigurationException(
                    "El proveedor " + providerId + " no es compatible con el tipo "
                            + type.getName());
        }

        defaultProviderByType.put(type, providerId);
    }

    @Override
    public boolean hasProvider(String providerId) {
        return providersById.containsKey(providerId);
    }

    @Override
    public boolean hasDefaultProvider(Class<? extends Notification> type) {
        return defaultProviderByType.containsKey(type);
    }

    @Override
    public String getDefaultProvider(Class<? extends Notification> type) {
        return defaultProviderByType.get(type);
    }

    @Override
    public NotificationResult send(Notification notification) {
        Objects.requireNonNull(notification, "notification no puede ser nula");

        @SuppressWarnings("unchecked")
        Class<? extends Notification> type = (Class<? extends Notification>) notification.getClass();

        String providerId = defaultProviderByType.get(type);
        if (providerId == null) {
            throw new NotificationConfigurationException(
                    "No hay proveedor predeterminado configurado para el tipo: " + type.getName());
        }

        ProviderEntry entry = providersById.get(providerId);
        if (entry == null) {
            throw new NotificationConfigurationException(
                    "El proveedor predeterminado " + providerId + " no existe para el tipo: "
                            + type.getName());
        }

        if (!entry.sender.notificationType().isInstance(notification)) {
            throw new NotificationConfigurationException(
                    "El proveedor " + providerId + " no acepta instancias de "
                            + notification.getClass().getName());
        }

        @SuppressWarnings("unchecked")
        NotificationSender<Notification> typedSender =
                (NotificationSender<Notification>) entry.sender;
        return typedSender.send(notification);
    }
}
