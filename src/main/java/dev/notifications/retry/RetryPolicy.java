package dev.notifications.retry;

import dev.notifications.model.NotificationResult;

import java.time.Duration;

/**
 * Política de reintentos para el envío de notificaciones.
 *
 * <p>Define cuántos intentos se realizan, cuándo reintentar tras un resultado
 * fallido y cuánto esperar entre intentos.
 *
 * <p>No se reintenta ante excepciones de validaci\u00f3n ni de configuraci\u00f3n,
 * ya que representan errores permanentes que no se resuelven con un nuevo intento.
 */
public interface RetryPolicy {

    /**
     * Cantidad total de intentos (incluido el primero). 1 significa sin reintentos.
     *
     * @return cantidad de intentos
     */
    int maxAttempts();

    /**
     * Indica si se debe reintentar tras recibir un resultado fallido.
     *
     * @param result       resultado del intento previo
     * @param attemptNumber n\u00famero del intento que acaba de fallar (comienza en 1)
     * @return {@code true} si se debe realizar otro intento
     */
    boolean shouldRetry(NotificationResult result, int attemptNumber);

    /**
     * Tiempo de espera antes del siguiente intento.
     *
     * @param attemptNumber n\u00famero del intento que acaba de fallar (comienza en 1)
     * @return duraci\u00f3n de espera
     */
    Duration delayBeforeRetry(int attemptNumber);
}
