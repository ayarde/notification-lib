package dev.notifications.retry;

import dev.notifications.api.NotificationClient;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.exception.NotificationSendingException;
import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;

import java.time.Duration;

/**
 * Cliente de notificaciones que aplica una política de reintentos.
 *
 * <p>Envuelve un {@link NotificationClient} y reintenta el envío cuando el
 * resultado es {@code FAILED} según la política configurada. No reintenta ante
 * {@link NotificationValidationException} ni {@link NotificationConfigurationException},
 * ya que representan errores permanentes.
 *
 * <p>{@code SENT} significa aceptación por el adaptador/proveedor; no implica
 * entrega ni lectura por el destinatario, incluso tras reintentos.
 */
public class RetryingNotificationClient implements NotificationClient {

    private final NotificationClient delegate;
    private final RetryPolicy retryPolicy;

    /**
     * Crea un cliente con reintentos.
     *
     * @param delegate     cliente a envolver
     * @param retryPolicy  política de reintentos
     */
    public RetryingNotificationClient(NotificationClient delegate, RetryPolicy retryPolicy) {
        this.delegate = delegate;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public NotificationResult send(Notification notification) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                NotificationResult result = delegate.send(notification);
                if (result.isSent() || !retryPolicy.shouldRetry(result, attempt)) {
                    return result;
                }
                sleep(retryPolicy.delayBeforeRetry(attempt));
            } catch (NotificationValidationException | NotificationConfigurationException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NotificationSendingException("Reintento interrumpido", e);
            }
        }
    }

    void sleep(Duration delay) throws InterruptedException {
        Thread.sleep(delay.toMillis());
    }
}
