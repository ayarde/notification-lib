package dev.notifications.validation;

import dev.notifications.model.Notification;

/**
 * Validador de notificaciones por tipo.
 *
 * <p>Cada validador declara el tipo que valida mediante {@link #forType()}. El validador
 * predeterminado delega en el validador registrado para cada tipo, sin un switch centralizado.
 */
public interface NotificationValidator {

    Class<? extends Notification> forType();

    void validate(Notification notification);
}
