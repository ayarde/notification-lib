package dev.notifications.retry;

import dev.notifications.api.NotificationClient;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.exception.NotificationSendingException;
import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link RetryingNotificationClient}.
 */
class RetryingNotificationClientTest {

    private static NotificationResult sentResult() {
        return NotificationResult.sent("msg-ok");
    }

    private static NotificationResult failedResult() {
        return NotificationResult.failed("EC001", "proveedor no disponible");
    }

    private NotificationClient clientQueSiempreDevuelve(NotificationResult resultado) {
        return notification -> resultado;
    }

    private NotificationClient clientQueDevuelveEntonces(NotificationResult primero,
                                                         NotificationResult segundo) {
        AtomicInteger llamadas = new AtomicInteger(0);
        return notification -> {
            int llamada = llamadas.incrementAndGet();
            return llamada == 1 ? primero : segundo;
        };
    }

    private NotificationClient clientQueSiempreFalla() {
        return notification -> failedResult();
    }

    @Test
    void resultadoSentSeDevuelveSinReintentos() {
        NotificationClient delegate = clientQueSiempreDevuelve(sentResult());
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        NotificationResult resultado = client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertTrue(resultado.isSent());
        assertEquals("msg-ok", resultado.providerMessageId());
    }

    @Test
    void resultadoFallidoSeReintentaHastaQueSuceda() {
        NotificationClient delegate = clientQueDevuelveEntonces(failedResult(), sentResult());
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        NotificationResult resultado = client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertTrue(resultado.isSent());
    }

    @Test
    void seAgotaronLosIntentosDevuelveUltimoResultadoFallido() {
        NotificationClient delegate = clientQueSiempreFalla();
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        NotificationResult resultado = client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertTrue(resultado.isFailed());
        assertEquals("EC001", resultado.errorCode());
        assertEquals("proveedor no disponible", resultado.errorMessage());
    }

    @Test
    void numeroDeIntentosRespetaMaxAttempts() {
        AtomicInteger llamadas = new AtomicInteger(0);
        NotificationClient delegate = notification -> {
            llamadas.incrementAndGet();
            return failedResult();
        };
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertEquals(3, llamadas.get());
    }

    @Test
    void sinReintentosConUnSoloIntento() {
        AtomicInteger llamadas = new AtomicInteger(0);
        NotificationClient delegate = notification -> {
            llamadas.incrementAndGet();
            return failedResult();
        };
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(1, Duration.ofMillis(1)));

        NotificationResult resultado = client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertTrue(resultado.isFailed());
        assertEquals(1, llamadas.get());
    }

    @Test
    void excepcionDeValidacionNoSeReintenta() {
        AtomicInteger llamadas = new AtomicInteger(0);
        NotificationClient delegate = notification -> {
            llamadas.incrementAndGet();
            throw new NotificationValidationException("email invalido");
        };
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        assertThrows(NotificationValidationException.class,
                () -> client.send(new EmailNotification("", "Asunto", "Cuerpo")));

        assertEquals(1, llamadas.get());
    }

    @Test
    void excepcionDeConfiguracionNoSeReintenta() {
        AtomicInteger llamadas = new AtomicInteger(0);
        NotificationClient delegate = notification -> {
            llamadas.incrementAndGet();
            throw new NotificationConfigurationException("proveedor no encontrado");
        };
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        assertThrows(NotificationConfigurationException.class,
                () -> client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));

        assertEquals(1, llamadas.get());
    }

    @Test
    void interrupcionDuranteReintentoLanzaNotificationSendingExceptionYConservaBandera() {
        Thread.currentThread().interrupt();

        AtomicInteger llamadas = new AtomicInteger(0);
        NotificationClient delegate = notification -> {
            llamadas.incrementAndGet();
            return failedResult();
        };
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        NotificationSendingException ex = assertThrows(NotificationSendingException.class,
                () -> client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));

        assertInstanceOf(InterruptedException.class, ex.getCause());
        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(1, llamadas.get());
    }

    @Test
    void notificacionNulaPropagaExcepcion() {
        NotificationClient delegate = notification -> {
            if (notification == null) {
                throw new NullPointerException("notification no puede ser nula");
            }
            return sentResult();
        };
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        assertThrows(NullPointerException.class, () -> client.send(null));
    }

    @Test
    void interrupcionDuranteReintentoSePropagaComoNotificationSendingException() {
        NotificationClient delegate = notification -> failedResult();
        RetryingNotificationClient client = new RetryingNotificationClient(
                delegate, new FixedDelayRetryPolicy(3, Duration.ofSeconds(10))) {
            @Override
            void sleep(Duration delay) throws InterruptedException {
                throw new InterruptedException("interrumpido");
            }
        };

        NotificationSendingException excepcion = assertThrows(NotificationSendingException.class,
                () -> client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));

        assertTrue(Thread.currentThread().isInterrupted(),
                "la bandera de interrupción debe quedar activa");
        assertEquals("Reintento interrumpido", excepcion.getMessage());
    }
}
