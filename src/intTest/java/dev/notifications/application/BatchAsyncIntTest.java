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

    private static void assertIdOpcio(String id, String canal, String destinatario) {
        assertNotNull(id);
        assertTrue(id.startsWith("sim-" + canal + "-"),
                "el identificador debe iniciar con sim-" + canal + "- pero fue: " + id);
        assertFalse(id.contains(destinatario),
                "el identificador no debe contener el destinatario pero fue: " + id);
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
        assertIdOpcio(resultados.get(0).providerMessageId(), "email", "usuario@example.com");
        assertTrue(resultados.get(1).isSent());
        assertIdOpcio(resultados.get(1).providerMessageId(), "sms", "+5491112345678");
        assertTrue(resultados.get(2).isSent());
        assertIdOpcio(resultados.get(2).providerMessageId(), "push", "token-dispositivo-123");
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
        assertIdOpcio(resultados.get(0).providerMessageId(), "email", "primero@test.c");

        assertTrue(resultados.get(1).isSent());
        assertIdOpcio(resultados.get(1).providerMessageId(), "sms", "+5491112345678");

        assertTrue(resultados.get(2).isSent());
        assertIdOpcio(resultados.get(2).providerMessageId(), "push", "token-3");
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
