package dev.notifications.retry;

import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link FixedDelayRetryPolicy}.
 */
class FixedDelayRetryPolicyTest {

    @Test
    void maxAttemptsMenorQueUnoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new FixedDelayRetryPolicy(0, Duration.ofMillis(10)));
        assertThrows(IllegalArgumentException.class,
                () -> new FixedDelayRetryPolicy(-1, Duration.ofMillis(10)));
    }

    @Test
    void delayNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new FixedDelayRetryPolicy(3, Duration.ofMillis(-1)));
    }

    @Test
    void delayNuloLanzaExcepcion() {
        assertThrows(NullPointerException.class,
                () -> new FixedDelayRetryPolicy(3, null));
    }

    @Test
    void unIntentoSignificaSinReintentos() {
        RetryPolicy policy = new FixedDelayRetryPolicy(1, Duration.ofMillis(100));
        NotificationResult failed = NotificationResult.failed("EC001", "fallo");

        assertFalse(policy.shouldRetry(failed, 1));
    }

    @Test
    void reintentaMientrasElResultadoSeaFallidoYQuedenIntentos() {
        RetryPolicy policy = new FixedDelayRetryPolicy(3, Duration.ofMillis(10));
        NotificationResult failed = NotificationResult.failed("EC001", "fallo");

        assertTrue(policy.shouldRetry(failed, 1));
        assertTrue(policy.shouldRetry(failed, 2));
        assertFalse(policy.shouldRetry(failed, 3));
    }

    @Test
    void noReintentaSiResultadoEsSent() {
        RetryPolicy policy = new FixedDelayRetryPolicy(3, Duration.ofMillis(10));
        NotificationResult sent = NotificationResult.sent("msg-1");

        assertFalse(policy.shouldRetry(sent, 1));
        assertFalse(policy.shouldRetry(sent, 2));
    }

    @Test
    void delayBeforeRetryDevuelveElDelayConfigurado() {
        Duration delay = Duration.ofSeconds(5);
        RetryPolicy policy = new FixedDelayRetryPolicy(3, delay);

        assertEquals(delay, policy.delayBeforeRetry(1));
        assertEquals(delay, policy.delayBeforeRetry(2));
    }
}
