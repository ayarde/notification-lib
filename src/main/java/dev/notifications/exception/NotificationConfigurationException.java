package dev.notifications.exception;

/**
 * Excepción lanzada cuando la configuración de la librería es inválida:
 * proveedor ausente, registro ambiguo, duplicados o referencias inexistentes.
 */
public class NotificationConfigurationException extends RuntimeException {

    public NotificationConfigurationException(String message) {
        super(message);
    }

    public NotificationConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationConfigurationException(Throwable cause) {
        super(cause);
    }
}
