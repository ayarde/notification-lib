package dev.notifications.retry;

import dev.notifications.model.NotificationResult;

import java.time.Duration;
import java.util.Objects;

/**
 * Pol\u00edtica de reintentos con retardo fijo entre intentos.
 *
 * <p>Reintenta mientras el resultado sea {@code FAILED} y no se haya alcanzado
 * la cantidad m\u00e1xima de intentos. El retardo entre intentos es constante.
 */
public record FixedDelayRetryPolicy(int maxAttempts, Duration delayBetweenAttempts) implements RetryPolicy {

    public FixedDelayRetryPolicy {
        Objects.requireNonNull(delayBetweenAttempts, "delayBetweenAttempts no puede ser nulo");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts debe ser al menos 1");
        }
        if (delayBetweenAttempts.isNegative()) {
            throw new IllegalArgumentException("delayBetweenAttempts no puede ser negativo");
        }
    }

    @Override
    public boolean shouldRetry(NotificationResult result, int attemptNumber) {
        return attemptNumber < maxAttempts && result.isFailed();
    }

    @Override
    public Duration delayBeforeRetry(int attemptNumber) {
        return delayBetweenAttempts;
    }
}
