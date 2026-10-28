package dev.notifications.exception;

/**
 * Excepción lanzada cuando una notificación o sus campos no superan la validación.
 * Representa un error de entrada inválida detectado antes de invocar el adaptador.
 */
public class NotificationValidationException extends RuntimeException {

    public NotificationValidationException(String message) {
        super(message);
    }

    public NotificationValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationValidationException(Throwable cause) {
        super(cause);
    }
}
