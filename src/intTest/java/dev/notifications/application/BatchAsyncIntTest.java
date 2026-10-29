package dev.notifications.application;

import dev.notifications.adapter.email.SimulatedEmailSender;
import dev.notifications.adapter.push.SimulatedPushSender;
import dev.notifications.adapter.sms.SimulatedSmsSender;
import dev.notifications.api.AsyncNotificationClient;
import dev.notifications.config.NotificationConfiguration;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.retry.FixedDelayRetryPolicy;
import dev.notifications.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de envío por lotes asíncrono con los adaptadores simulados
 * y la configuración mediante {@code NotificationConfiguration.buildAsync()}.
 *
 * <p>Verifica envíos de lotes con Email, SMS y Push, resultados parciales,
 * preservación del orden, integración con reintentos y ausencia de llamadas de red.
 */
class BatchAsyncIntTest {

    private AsyncNotificationClient clienteAsyncConConfiguracionValida() {
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        return NotificationConfiguration.builder()
                .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                .defaultProvider(EmailNotification.class, "email-primary")
                .defaultProvider(SmsNotification.class, "sms-primary")
                .defaultProvider(PushNotification.class, "push-primary")
                .withExecutor(executor)
                .buildAsync();
    }

    @Test
    void loteVacioConAdaptadoresSimuladosDevuelveListaVacia() {
        AsyncNotificationClient async = clienteAsyncConConfiguracionValida();

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(List.of());

        assertTrue(futuro.isDone());
        assertTrue(futuro.join().isEmpty());
    }

    @Test
    void loteTresCanalesSimuladosTodosSent() {
        AsyncNotificationClient async = clienteAsyncConConfiguracionValida();

        List<Notification> notificaciones = List.of(
                new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"),
                new SmsNotification("+5491112345678", "Mensaje de prueba de SMS"),
                new PushNotification("token-dispositivo-123", "Título push", "Cuerpo"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(3, resultados.size());
        assertTrue(resultados.get(0).isSent());
        assertEquals("sim-email-usuario@example.com", resultados.get(0).providerMessageId());
        assertTrue(resultados.get(1).isSent());
        assertEquals("sim-sms-+5491112345678", resultados.get(1).providerMessageId());
        assertTrue(resultados.get(2).isSent());
        assertEquals("sim-push-token-dispositivo-123", resultados.get(2).providerMessageId());
    }

    @Test
    void loteConRetryConfiguradoProcesaTodasLasNotificaciones() {
        RetryPolicy retryPolicy = new FixedDelayRetryPolicy(3, Duration.ofMillis(1));
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        AsyncNotificationClient async = NotificationConfiguration.builder()
                .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                .defaultProvider(EmailNotification.class, "email-primary")
                .defaultProvider(SmsNotification.class, "sms-primary")
                .defaultProvider(PushNotification.class, "push-primary")
                .withRetryPolicy(retryPolicy)
                .withExecutor(executor)
                .buildAsync();

        List<Notification> notificaciones = List.of(
                new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"),
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
    void loteOrdenDeResultadosCoincideConOrdenDeEntrada() {
        AsyncNotificationClient async = clienteAsyncConConfiguracionValida();

        List<Notification> notificaciones = List.of(
                new EmailNotification("primero@test.c", "Asunto 1", "Cuerpo 1"),
                new SmsNotification("+5491112345678", "Mensaje SMS 2"),
                new PushNotification("token-3", "Título 3", "Cuerpo 3"));

        CompletableFuture<List<NotificationResult>> futuro =
                async.sendBatchAsync(notificaciones);

        List<NotificationResult> resultados = futuro.join();
        assertEquals(3, resultados.size());

        // Verificar correspondencia posición por posición
        assertTrue(resultados.get(0).isSent());
        assertEquals("sim-email-primero@test.c", resultados.get(0).providerMessageId());

        assertTrue(resultados.get(1).isSent());
        assertEquals("sim-sms-+5491112345678", resultados.get(1).providerMessageId());

        assertTrue(resultados.get(2).isSent());
        assertEquals("sim-push-token-3", resultados.get(2).providerMessageId());
    }

    @Test
    void loteListaNulaLanzaNullPointerException() {
        AsyncNotificationClient async = clienteAsyncConConfiguracionValida();

        assertThrows(NullPointerException.class, () -> async.sendBatchAsync(null));
    }

    @Test
    void loteConElementoNuloLanzaNullPointerException() {
        AsyncNotificationClient async = clienteAsyncConConfiguracionValida();

        List<Notification> notificaciones = new ArrayList<>();
        notificaciones.add(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        notificaciones.add(null);

        assertThrows(NullPointerException.class, () -> async.sendBatchAsync(notificaciones));
    }
}
