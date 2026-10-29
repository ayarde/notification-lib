package dev.notifications.application;

import dev.notifications.api.AsyncNotificationClient;
import dev.notifications.api.NotificationClient;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.exception.NotificationSendingException;
import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.retry.FixedDelayRetryPolicy;
import dev.notifications.retry.RetryingNotificationClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link DefaultAsyncNotificationService}.
 */
class DefaultAsyncNotificationServiceTest {

    private static NotificationResult sentResult() {
        return NotificationResult.sent("msg-async-ok");
    }

    private static NotificationResult failedResult() {
        return NotificationResult.failed("EC001", "proveedor no disponible");
    }

    private NotificationClient syncClientThatReturns(NotificationResult resultado) {
        return notification -> resultado;
    }

    private NotificationClient syncClientThatThrowsValidation() {
        return notification -> {
            throw new NotificationValidationException("email inválido");
        };
    }

    private NotificationClient syncClientThatThrowsConfig() {
        return notification -> {
            throw new NotificationConfigurationException("proveedor no encontrado");
        };
    }

    private NotificationClient syncClientThatThrowsUnexpected() {
        return notification -> {
            throw new IllegalStateException("red caída");
        };
    }

    private Executor directExecutor() {
        return Runnable::run;
    }

    @Test
    void envioAsyncExitosoCompletaConResultadoSent() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<NotificationResult> futuro = async.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertTrue(futuro.isDone());
        NotificationResult resultado = futuro.join();
        assertTrue(resultado.isSent());
        assertEquals("msg-async-ok", resultado.providerMessageId());
    }

    @Test
    void resultadoFailedSeEntregaNormalmenteSinExcepcion() {
        NotificationClient delegate = syncClientThatReturns(failedResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<NotificationResult> futuro = async.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        NotificationResult resultado = futuro.join();
        assertTrue(resultado.isFailed());
        assertEquals("EC001", resultado.errorCode());
        assertEquals("proveedor no disponible", resultado.errorMessage());
    }

    @Test
    void excepcionValidacionCompletaFuturoExcepcionalmente() {
        NotificationClient delegate = syncClientThatThrowsValidation();
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<NotificationResult> futuro = async.sendAsync(
                new EmailNotification("", "Asunto", "Cuerpo"));

        CompletionException excepcion = assertThrows(CompletionException.class, futuro::join);
        assertInstanceOf(NotificationValidationException.class, excepcion.getCause());
        assertEquals("email inválido", excepcion.getCause().getMessage());
    }

    @Test
    void excepcionConfiguracionCompletaFuturoExcepcionalmente() {
        NotificationClient delegate = syncClientThatThrowsConfig();
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<NotificationResult> futuro = async.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        CompletionException excepcion = assertThrows(CompletionException.class, futuro::join);
        assertInstanceOf(NotificationConfigurationException.class, excepcion.getCause());
        assertEquals("proveedor no encontrado", excepcion.getCause().getMessage());
    }

    @Test
    void excepcionInesperadaSeEnvuelveEnNotificationSendingException() {
        NotificationClient delegate = syncClientThatThrowsUnexpected();
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<NotificationResult> futuro = async.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        CompletionException excepcion = assertThrows(CompletionException.class, futuro::join);
        assertInstanceOf(NotificationSendingException.class, excepcion.getCause());
        assertInstanceOf(IllegalStateException.class, excepcion.getCause().getCause());
        assertEquals("red caída", excepcion.getCause().getCause().getMessage());
    }

    @Test
    void notificacionNulaLanzaNullPointerException() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        assertThrows(NullPointerException.class, () -> async.sendAsync(null));
    }

    @Test
    void ejecutorPersonalizadoSeUtilizaParaEnvio() {
        AtomicInteger ejecuciones = new AtomicInteger(0);
        Executor ejecutorPersonalizado = tarea -> {
            ejecuciones.incrementAndGet();
            tarea.run();
        };
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorPersonalizado);

        async.sendAsync(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertEquals(1, ejecuciones.get());
    }

    @Test
    void delegateNuloLanzaNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new DefaultAsyncNotificationService(null, directExecutor()));
    }

    @Test
    void executorNuloLanzaNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new DefaultAsyncNotificationService(syncClientThatReturns(sentResult()), null));
    }

    @Test
    void envioAsyncConExecutorQueRechazaLanzaExcepcionSincronicamente() {
        Executor ejecutorQueRechaza = tarea -> {
            throw new RejectedExecutionException("executor saturado");
        };
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorQueRechaza);

        assertThrows(RejectedExecutionException.class,
                () -> async.sendAsync(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));
    }

    @Test
    void resultadoAsyncNoEsNullCuandoDelegateDevuelveSent() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<NotificationResult> futuro = async.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        NotificationResult resultado = futuro.join();
        assertNotNull(resultado);
        assertEquals(NotificationStatus.SENT, resultado.status());
    }

    @Test
    void asyncNotificationClientEsInterfazDistintaDeSync() {
        assertNotSame(NotificationClient.class, AsyncNotificationClient.class);
        // Un cliente asíncrono no es un cliente síncrono y viceversa.
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());
        assertInstanceOf(AsyncNotificationClient.class, async);
    }

    // --- Pruebas de envío por lotes ---

    @Test
    void loteVacioDevuelveListaVacia() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(List.of());

        assertTrue(futuro.isDone());
        assertTrue(futuro.join().isEmpty());
    }

    @Test
    void loteConUnaNotificacionDevuelveUnResultado() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(List.of(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));

        List<NotificationResult> resultados = futuro.join();
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).isSent());
        assertEquals("msg-async-ok", resultados.get(0).providerMessageId());
    }

    @Test
    void loteVariasNotificacionesExitosasDevuelveTodosSent() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        List<Notification> notificaciones = List.of(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"),
                new SmsNotification("+5491112345678", "Mensaje SMS"),
                new PushNotification("token-1", "Título", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(3, resultados.size());
        for (NotificationResult resultado : resultados) {
            assertTrue(resultado.isSent());
        }
    }

    @Test
    void loteResultadosParcialesConservaExitosYFallos() {
        NotificationResult resultadoFallido = NotificationResult.failed("EC001", "proveedor no disponible");
        NotificationClient delegate = notification -> {
            if (notification.recipient().equals("fallo@test.c")) {
                return resultadoFallido;
            }
            return sentResult();
        };
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        List<Notification> notificaciones = List.of(
                new EmailNotification("ok@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("fallo@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("ok2@test.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(3, resultados.size());
        assertTrue(resultados.get(0).isSent());
        assertTrue(resultados.get(1).isFailed());
        assertEquals("EC001", resultados.get(1).errorCode());
        assertEquals("proveedor no disponible", resultados.get(1).errorMessage());
        assertTrue(resultados.get(2).isSent());
    }

    @Test
    void loteListaNulaLanzaNullPointerExceptionSincronica() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        assertThrows(NullPointerException.class, () -> async.sendBatchAsync(null));
    }

    @Test
    void loteConElementoNuloLanzaNullPointerExceptionSincronica() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        List<Notification> notificaciones = new ArrayList<>();
        notificaciones.add(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        notificaciones.add(null);

        assertThrows(NullPointerException.class, () -> async.sendBatchAsync(notificaciones));
    }

    @Test
    void loteValidacionCompletaAntesDeProgramarTareas() {
        AtomicInteger tareasProgramadas = new AtomicInteger(0);
        Executor executorContador = tarea -> {
            tareasProgramadas.incrementAndGet();
            tarea.run();
        };
        NotificationClient delegate = notification -> {
            throw new NotificationValidationException("email inválido");
        };
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, executorContador);

        List<Notification> notificaciones = new ArrayList<>();
        notificaciones.add(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        notificaciones.add(null);

        assertThrows(NullPointerException.class, () -> async.sendBatchAsync(notificaciones));

        // Ninguna tarea debió programarse porque la validación falla antes
        assertEquals(0, tareasProgramadas.get());
    }

    @Test
    void loteNotificacionInvalidaDevuelveResultadoFallidoEnPosicion() {
        NotificationResult resultadoFallido = NotificationResult.failed("EC001", "proveedor no disponible");
        NotificationClient delegate = notification -> {
            if (notification.recipient().equals("fallo@test.c")) {
                throw new NotificationValidationException("email inválido");
            }
            return resultadoFallido;
        };
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        List<Notification> notificaciones = List.of(
                new EmailNotification("fallo@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("ok@test.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(2, resultados.size());
        assertTrue(resultados.get(0).isFailed());
        assertEquals("ERROR_ENVIO", resultados.get(0).errorCode());
        assertTrue(resultados.get(1).isFailed());
        assertEquals("EC001", resultados.get(1).errorCode());
    }

    @Test
    void loteExcepcionInesperadaSeConvierteEnResultadoFallido() {
        NotificationClient delegate = notification -> {
            throw new IllegalStateException("red caída");
        };
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        List<Notification> notificaciones = List.of(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).isFailed());
        assertEquals("ERROR_ENVIO", resultados.get(0).errorCode());
        assertEquals("red caída", resultados.get(0).errorMessage());
    }

    @Test
    void loteConservaOrdenAunqueTareasTerminenFueraDeOrden() {
        AtomicInteger llamadas = new AtomicInteger(0);
        NotificationClient delegate = notification -> {
            int llamada = llamadas.incrementAndGet();
            // Simular que la segunda notificación tarda más (out of order)
            if (llamada == 2) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return sentResult();
        };
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        List<Notification> notificaciones = List.of(
                new EmailNotification("primero@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("segundo@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("tercero@test.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(3, resultados.size());
        assertTrue(resultados.get(0).isSent());
        assertTrue(resultados.get(1).isSent());
        assertTrue(resultados.get(2).isSent());
    }

    @Test
    void loteExecutorPersonalizadoSeUtilizaParaCadaNotificacion() {
        AtomicInteger ejecuciones = new AtomicInteger(0);
        Executor ejecutorPersonalizado = tarea -> {
            ejecuciones.incrementAndGet();
            tarea.run();
        };
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorPersonalizado);

        List<Notification> notificaciones = List.of(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"),
                new EmailNotification("b@b.c", "Asunto", "Cuerpo"));

        async.sendBatchAsync(notificaciones);

        assertEquals(2, ejecuciones.get());
    }

    @Test
    void loteExecutorRechazaPrimeraTareaDevuelveResultadoFallidoSinExcepcion() {
        Executor ejecutorQueRechaza = tarea -> {
            throw new RejectedExecutionException("executor saturado");
        };
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorQueRechaza);

        List<Notification> notificaciones = List.of(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).isFailed());
        assertEquals("ERROR_EXECUTOR_RECHAZADO", resultados.get(0).errorCode());
        assertFalse(resultados.get(0).errorMessage().isBlank());
    }

    @Test
    void loteConRetryNotificacionesExitosasNoSeReenvian() {
        AtomicInteger intentos = new AtomicInteger(0);
        NotificationClient delegateConFalloLuegoExito = notification -> {
            int i = intentos.incrementAndGet();
            if (i < 3) {
                return NotificationResult.failed("EC001", "fallo transitorio");
            }
            return NotificationResult.sent("msg-ok");
        };
        RetryingNotificationClient clienteConReintentos =
                new RetryingNotificationClient(
                        delegateConFalloLuegoExito,
                        new FixedDelayRetryPolicy(5, Duration.ofMillis(1)));

        Executor executor = Runnable::run;
        AsyncNotificationClient async = new DefaultAsyncNotificationService(
                clienteConReintentos, executor);

        List<Notification> notificaciones = List.of(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).isSent());
        assertEquals("msg-ok", resultados.get(0).providerMessageId());
        assertTrue(intentos.get() >= 3);
    }

    @Test
    void loteConRetryReintentosIndependientesPorNotificacion() {
        AtomicInteger intentosNotificacion1 = new AtomicInteger(0);
        AtomicInteger intentosNotificacion2 = new AtomicInteger(0);
        NotificationClient delegate = notification -> {
            if (notification.recipient().equals("primero@test.c")) {
                int i = intentosNotificacion1.incrementAndGet();
                if (i < 2) {
                    return NotificationResult.failed("EC001", "fallo");
                }
                return NotificationResult.sent("msg-1");
            } else {
                int i = intentosNotificacion2.incrementAndGet();
                return NotificationResult.sent("msg-2");
            }
        };
        RetryingNotificationClient clienteConReintentos =
                new RetryingNotificationClient(
                        delegate,
                        new FixedDelayRetryPolicy(3, Duration.ofMillis(1)));

        Executor executor = Runnable::run;
        AsyncNotificationClient async = new DefaultAsyncNotificationService(
                clienteConReintentos, executor);

        List<Notification> notificaciones = List.of(
                new EmailNotification("primero@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("segundo@test.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(2, resultados.size());
        assertTrue(resultados.get(0).isSent());
        assertEquals("msg-1", resultados.get(0).providerMessageId());
        assertTrue(resultados.get(1).isSent());
        assertEquals("msg-2", resultados.get(1).providerMessageId());
        assertEquals(2, intentosNotificacion1.get());
        assertEquals(1, intentosNotificacion2.get());
    }

    @Test
    void loteConsiderableMantieneOrdenYCompleta() {
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, directExecutor());

        List<Notification> notificaciones = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            notificaciones.add(new EmailNotification("usuario" + i + "@test.c", "Asunto", "Cuerpo"));
        }

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(50, resultados.size());
        for (int i = 0; i < 50; i++) {
            assertTrue(resultados.get(i).isSent(), "La notificación en posición " + i + " debe ser exitosa");
        }
    }

    // --- Pruebas de rechazo del Executor ---

    @Test
    void loteExecutorAceptaPrimeraYRechazaSegundaDevuelveResultadosMixto() {
        AtomicInteger contador = new AtomicInteger(0);
        Executor ejecutorParcial = tarea -> {
            int c = contador.incrementAndGet();
            if (c == 1) {
                tarea.run(); // acepta la primera
            } else {
                throw new RejectedExecutionException("executor saturado"); // rechaza la segunda
            }
        };
        NotificationResult resultadoSent = NotificationResult.sent("msg-ok");
        NotificationClient delegate = notification -> resultadoSent;
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorParcial);

        List<Notification> notificaciones = List.of(
                new EmailNotification("primero@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("segundo@test.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(2, resultados.size());
        // Primera notificación fue aceptada y ejecutada
        assertTrue(resultados.get(0).isSent());
        assertEquals("msg-ok", resultados.get(0).providerMessageId());
        // Segunda notificación no pudo programarse
        assertTrue(resultados.get(1).isFailed());
        assertEquals("ERROR_EXECUTOR_RECHAZADO", resultados.get(1).errorCode());
    }

    @Test
    void loteExecutorRechazaTodasLasTareasDevuelveTodosFallidos() {
        Executor ejecutorQueRechaza = tarea -> {
            throw new RejectedExecutionException("executor saturado");
        };
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorQueRechaza);

        List<Notification> notificaciones = List.of(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"),
                new EmailNotification("b@b.c", "Asunto", "Cuerpo"),
                new EmailNotification("c@b.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(3, resultados.size());
        for (NotificationResult resultado : resultados) {
            assertTrue(resultado.isFailed());
            assertEquals("ERROR_EXECUTOR_RECHAZADO", resultado.errorCode());
        }
    }

    @Test
    void loteExecutorRechazoParcialMantieneOrdenDeEntrada() {
        AtomicInteger contador = new AtomicInteger(0);
        Executor ejecutorParcial = tarea -> {
            int c = contador.incrementAndGet();
            if (c % 2 == 1) {
                tarea.run(); // acepta impares (1, 3, 5...)
            } else {
                throw new RejectedExecutionException("executor saturado"); // rechaza pares (2, 4...)
            }
        };
        NotificationResult resultadoSent = NotificationResult.sent("msg-ok");
        NotificationClient delegate = notification -> resultadoSent;
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorParcial);

        List<Notification> notificaciones = List.of(
                new EmailNotification("n1@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("n2@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("n3@test.c", "Asunto", "Cuerpo"),
                new EmailNotification("n4@test.c", "Asunto", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(4, resultados.size());
        // Posición 0: aceptada -> SENT
        assertTrue(resultados.get(0).isSent());
        // Posición 1: rechazada -> FAILED
        assertTrue(resultados.get(1).isFailed());
        assertEquals("ERROR_EXECUTOR_RECHAZADO", resultados.get(1).errorCode());
        // Posición 2: aceptada -> SENT
        assertTrue(resultados.get(2).isSent());
        // Posición 3: rechazada -> FAILED
        assertTrue(resultados.get(3).isFailed());
        assertEquals("ERROR_EXECUTOR_RECHAZADO", resultados.get(3).errorCode());
    }

    @Test
    void sendAsyncSinCambiosConExecutorQueRechaza() {
        Executor ejecutorQueRechaza = tarea -> {
            throw new RejectedExecutionException("executor saturado");
        };
        NotificationClient delegate = syncClientThatReturns(sentResult());
        AsyncNotificationClient async = new DefaultAsyncNotificationService(delegate, ejecutorQueRechaza);

        assertThrows(RejectedExecutionException.class,
                () -> async.sendAsync(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));
    }
}
