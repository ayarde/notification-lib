package dev.notifications.application;

import dev.notifications.api.NotificationClient;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.validation.NotificationValidator;

/**
 * Servicio de aplicación predeterminado.
 *
 * <p>Responsabilidades: recibir la notificación, validarla, delegar en el registro de
 * adaptadores y devolver el resultado. No construye SDK de proveedores, no contiene un
 * switch por proveedor y no oculta errores devolviendo siempre éxito.
 */
public class DefaultNotificationService implements NotificationClient {

    private final NotificationValidator validator;
    private final NotificationSenderRegistry registry;

    public DefaultNotificationService(NotificationValidator validator,
                                      NotificationSenderRegistry registry) {
        this.validator = validator;
        this.registry = registry;
    }

    @Override
    public NotificationResult send(Notification notification) {
        validator.validate(notification);
        return registry.send(notification);
    }
}
