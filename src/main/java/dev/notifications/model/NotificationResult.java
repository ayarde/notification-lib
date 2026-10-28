package dev.notifications.model;

import java.util.Objects;

/**
 * Resultado de un intento de envío de notificación.
 *
 * <p>Invariantes garantizadas:
 * <ul>
 *   <li>{@code SENT}: no contiene {@code errorCode} ni {@code errorMessage} y puede incluir un
 *       {@code providerMessageId}.</li>
 *   <li>{@code FAILED}: contiene {@code errorCode} y {@code errorMessage} no vacíos, y no se
 *       presenta como envío exitoso.</li>
 * </ul>
 *
 * <p>{@code SENT} significa aceptación por el adaptador/proveedor; no implica entrega ni lectura
 * por el destinatario.
 */
public record NotificationResult(
        NotificationStatus status,
        String providerMessageId,
        String errorCode,
        String errorMessage
) {

    public NotificationResult {
        Objects.requireNonNull(status, "status no puede ser nulo");

        switch (status) {
            case SENT -> {
                if (errorCode != null || errorMessage != null) {
                    throw new IllegalArgumentException(
                            "Un resultado SENT no puede contener errorCode ni errorMessage");
                }
            }
            case FAILED -> {
                if (errorCode == null || errorCode.isBlank()) {
                    throw new IllegalArgumentException(
                            "Un resultado FAILED requiere errorCode no vacío");
                }
                if (errorMessage == null || errorMessage.isBlank()) {
                    throw new IllegalArgumentException(
                            "Un resultado FAILED requiere errorMessage no vacío");
                }
            }
            default -> throw new IllegalArgumentException(
                    "Estado de notificación desconocido: " + status);
        }
    }

    /**
     * Crea un resultado de éxito (aceptación por el proveedor).
     *
     * @param providerMessageId identificador del mensaje en el proveedor, o {@code null}
     * @return resultado exitoso
     */
    public static NotificationResult sent(String providerMessageId) {
        return new NotificationResult(NotificationStatus.SENT, providerMessageId, null, null);
    }

    /**
     * Crea un resultado de éxito sin identificador de proveedor.
     *
     * @return resultado exitoso
     */
    public static NotificationResult sent() {
        return new NotificationResult(NotificationStatus.SENT, null, null, null);
    }

    /**
     * Crea un resultado de fallo.
     *
     * @param errorCode   código de error
     * @param errorMessage mensaje de error
     * @return resultado fallido
     */
    public static NotificationResult failed(String errorCode, String errorMessage) {
        return new NotificationResult(NotificationStatus.FAILED, null, errorCode, errorMessage);
    }

    /**
     * Indica si el resultado representa aceptación por el proveedor.
     *
     * <p>Nota: aceptación no implica entrega ni lectura.
     *
     * @return {@code true} si el estado es {@code SENT}
     */
    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }

    /**
     * Indica si el resultado representa un fallo de envío.
     *
     * @return {@code true} si el estado es {@code FAILED}
     */
    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }
}
