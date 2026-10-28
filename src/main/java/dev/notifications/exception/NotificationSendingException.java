package dev.notifications.exception;

/**
 * Excepción lanzada cuando ocurre un error inesperado que impide completar
 * el intento de envío. Conserva la causa original cuando corresponde.
 *
 * <p>Un fallo esperado del proveedor se representa mediante
 * {@link NotificationResult#failed(String, String)}, no mediante esta excepción.
 */
public class NotificationSendingException extends RuntimeException {

    public NotificationSendingException(String message) {
        super(message);
    }

    public NotificationSendingException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationSendingException(Throwable cause) {
        super(cause);
    }
}
