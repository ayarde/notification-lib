package dev.notifications.validation;

import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.model.Notification;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Validador predeterminado que delega en validadores registrados por tipo de notificación.
 *
 * <p>No existe un switch centralizado sobre Email, SMS, Push u otros canales. Agregar un nuevo
 * canal requiere registrar su validador sin modificar esta clase (Open/Closed).
 */
public class DefaultNotificationValidator implements NotificationValidator {

    private final Map<Class<? extends Notification>, NotificationValidator> validatorsByType = new HashMap<>();

    /**
     * Registra un validador para un tipo de notificación.
     *
     * @param validator validador a registrar
     * @throws NotificationConfigurationException si ya existe un validador para el mismo tipo
     */
    public void register(NotificationValidator validator) {
        Objects.requireNonNull(validator, "validator no puede ser nulo");
        Class<? extends Notification> type = validator.forType();
        if (validatorsByType.containsKey(type)) {
            throw new NotificationConfigurationException(
                    "Ya existe un validador registrado para el tipo: " + type.getName());
        }
        validatorsByType.put(type, validator);
    }

    /**
     * Indica si hay un validador registrado para el tipo de la notificación.
     */
    public boolean hasValidatorFor(Class<? extends Notification> type) {
        return validatorsByType.containsKey(type);
    }

    @Override
    public Class<? extends Notification> forType() {
        throw new UnsupportedOperationException(
                "DefaultNotificationValidator no valida un tipo concreto directamente");
    }

    @Override
    public void validate(Notification notification) {
        Objects.requireNonNull(notification, "notification no puede ser nula");

        Class<? extends Notification> type = notification.getClass();

        NotificationValidator validator = validatorsByType.get(type);
        if (validator == null) {
            throw new NotificationConfigurationException(
                    "No hay validador registrado para el tipo: " + type.getName());
        }

        validator.validate(notification);
    }
}
