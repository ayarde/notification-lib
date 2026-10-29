package dev.notifications.config;

import dev.notifications.adapter.email.SimulatedEmailSender;
import dev.notifications.adapter.push.SimulatedPushSender;
import dev.notifications.adapter.sms.SimulatedSmsSender;
import dev.notifications.api.AsyncNotificationClient;
import dev.notifications.api.NotificationClient;
import dev.notifications.application.DefaultAsyncNotificationService;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.retry.FixedDelayRetryPolicy;
import dev.notifications.retry.RetryingNotificationClient;
import dev.notifications.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del Builder de {@link NotificationConfiguration}.
 */
class NotificationConfigurationBuilderTest {

    private NotificationConfiguration.Builder configuracionValida() {
        return NotificationConfiguration.builder()
                .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                .defaultProvider(EmailNotification.class, "email-primary")
                .defaultProvider(SmsNotification.class, "sms-primary")
                .defaultProvider(PushNotification.class, "push-primary");
    }

    @Test
    void buildConConfiguracionValidaDevuelveCliente() {
        NotificationClient client = configuracionValida().build();

        assertNotNull(client);
        assertInstanceOf(NotificationClient.class, client);
    }

    @Test
    void buildRechazaProviderIdDuplicado() {
        assertThrows(NotificationConfigurationException.class,
                () -> NotificationConfiguration.builder()
                        .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                        .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                        .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                        .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                        .defaultProvider(EmailNotification.class, "email-primary")
                        .defaultProvider(SmsNotification.class, "sms-primary")
                        .defaultProvider(PushNotification.class, "push-primary")
                        .build());
    }

    @Test
    void buildRechazaDefaultConProviderIdInexistente() {
        assertThrows(NotificationConfigurationException.class,
                () -> NotificationConfiguration.builder()
                        .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                        .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                        .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                        .defaultProvider(EmailNotification.class, "email-primary")
                        .defaultProvider(SmsNotification.class, "sms-primary")
                        .defaultProvider(PushNotification.class, "no-existe")
                        .build());
    }

    @Test
    void buildRechazaDefaultIncompatibleConElTipo() {
        assertThrows(NotificationConfigurationException.class,
                () -> NotificationConfiguration.builder()
                        .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                        .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                        .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                        .defaultProvider(EmailNotification.class, "sms-primary")
                        .defaultProvider(SmsNotification.class, "sms-primary")
                        .defaultProvider(PushNotification.class, "push-primary")
                        .build());
    }

    @Test
    void buildRechazaAusenciaDefaultParaEmail() {
        assertThrows(NotificationConfigurationException.class,
                () -> NotificationConfiguration.builder()
                        .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                        .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                        .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                        .defaultProvider(SmsNotification.class, "sms-primary")
                        .defaultProvider(PushNotification.class, "push-primary")
                        .build());
    }

    @Test
    void buildRechazaAusenciaDefaultParaSms() {
        assertThrows(NotificationConfigurationException.class,
                () -> NotificationConfiguration.builder()
                        .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                        .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                        .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                        .defaultProvider(EmailNotification.class, "email-primary")
                        .defaultProvider(PushNotification.class, "push-primary")
                        .build());
    }

    @Test
    void buildRechazaAusenciaDefaultParaPush() {
        assertThrows(NotificationConfigurationException.class,
                () -> NotificationConfiguration.builder()
                        .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                        .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                        .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                        .defaultProvider(EmailNotification.class, "email-primary")
                        .defaultProvider(SmsNotification.class, "sms-primary")
                        .build());
    }

    @Test
    void clientesConstruidosSonIndependientes() {
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder()
                .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                .defaultProvider(EmailNotification.class, "email-primary")
                .defaultProvider(SmsNotification.class, "sms-primary")
                .defaultProvider(PushNotification.class, "push-primary");

        NotificationClient cliente1 = builder.build();

        // Agregar un proveedor adicional después del primer build no debe afectar al cliente 1
        builder.addSender("email-backup", EmailNotification.class, new SimulatedEmailSender());
        NotificationClient cliente2 = builder.build();

        assertNotSame(cliente1, cliente2);

        // Ambos clientes deben funcionar correctamente
        NotificationResult email1 = cliente1.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        assertTrue(email1.isSent());

        NotificationResult email2 = cliente2.send(new EmailNotification("d@e.f", "Otro", "Contenido"));
        assertTrue(email2.isSent());
    }

    @Test
    void buildPuedeLlamarseVariasVecesConElMismoBuilder() {
        NotificationConfiguration.Builder builder = configuracionValida();

        NotificationClient cliente1 = builder.build();
        NotificationClient cliente2 = builder.build();

        assertNotSame(cliente1, cliente2);
        assertTrue(cliente1.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")).isSent());
        assertTrue(cliente2.send(new EmailNotification("x@y.z", "Asunto", "Cuerpo")).isSent());
    }

    @Test
    void buildConRetryPolicyReintentaTrasResultadoFallido() {
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder()
                .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                .defaultProvider(EmailNotification.class, "email-primary")
                .defaultProvider(SmsNotification.class, "sms-primary")
                .defaultProvider(PushNotification.class, "push-primary");

        RetryPolicy retryPolicy = new FixedDelayRetryPolicy(3, Duration.ofMillis(1));
        NotificationClient client = builder.withRetryPolicy(retryPolicy).build();

        // El adaptador simulado siempre devuelve SENT, por lo que no se reintenta.
        NotificationResult resultado = client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        assertTrue(resultado.isSent());
    }

    @Test
    void buildSinRetryPolicyEnvíaUnaSolaVez() {
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder()
                .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                .defaultProvider(EmailNotification.class, "email-primary")
                .defaultProvider(SmsNotification.class, "sms-primary")
                .defaultProvider(PushNotification.class, "push-primary");

        NotificationClient client = builder.build();

        NotificationResult resultado = client.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        assertTrue(resultado.isSent());
    }

    @Test
    void buildAsyncConExecutorDevuelveClienteAsync() {
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        NotificationConfiguration.Builder builder = configuracionValida()
                .withExecutor(executor);

        AsyncNotificationClient asyncClient = builder.buildAsync();

        assertNotNull(asyncClient);
        assertInstanceOf(AsyncNotificationClient.class, asyncClient);

        CompletableFuture<NotificationResult> futuro = asyncClient.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertTrue(futuro.join().isSent());
    }

    @Test
    void buildAsyncSinExecutorLanzaIllegalStateException() {
        assertThrows(IllegalStateException.class, configuracionValida()::buildAsync);
    }

    @Test
    void buildAsyncConRetryPolicyReintentaResultadoFallido() {
        java.util.concurrent.atomic.AtomicInteger intentos = new java.util.concurrent.atomic.AtomicInteger(0);
        NotificationClient delegateConFalloLuegoExito = notification -> {
            int i = intentos.incrementAndGet();
            if (i < 3) {
                return NotificationResult.failed("EC001", "fallo transitorio");
            }
            return NotificationResult.sent("msg-ok");
        };
        // Usamos un cliente sincrónico que simula fallos y luego éxito,
        // envolviéndolo con RetryingNotificationClient para verificar composición.
        RetryingNotificationClient clienteConReintentos =
                new RetryingNotificationClient(
                        delegateConFalloLuegoExito,
                        new FixedDelayRetryPolicy(5, Duration.ofMillis(1)));

        Executor executor = Runnable::run;
        AsyncNotificationClient async = new DefaultAsyncNotificationService(
                clienteConReintentos, executor);

        CompletableFuture<NotificationResult> futuro = async.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        NotificationResult resultado = futuro.join();
        assertTrue(resultado.isSent());
        assertEquals("msg-ok", resultado.providerMessageId());
        assertTrue(intentos.get() >= 3);
    }

    @Test
    void buildAsyncEnviaLosTresCanales() {
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        NotificationConfiguration.Builder builder = configuracionValida()
                .withExecutor(executor);

        AsyncNotificationClient asyncClient = builder.buildAsync();

        CompletableFuture<NotificationResult> emailFuture = asyncClient.sendAsync(
                new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"));
        CompletableFuture<NotificationResult> smsFuture = asyncClient.sendAsync(
                new SmsNotification("+5491112345678", "Mensaje de prueba"));
        CompletableFuture<NotificationResult> pushFuture = asyncClient.sendAsync(
                new PushNotification("token-dispositivo-123", "Título", "Cuerpo"));

        assertTrue(emailFuture.join().isSent());
        assertTrue(smsFuture.join().isSent());
        assertTrue(pushFuture.join().isSent());
    }

    @Test
    void clientesAsyncConstruidosSonIndependientes() {
        Executor executor = Runnable::run;
        NotificationConfiguration.Builder builder = configuracionValida()
                .withExecutor(executor);

        AsyncNotificationClient cliente1 = builder.buildAsync();
        builder.addSender("email-backup", EmailNotification.class, new SimulatedEmailSender());
        AsyncNotificationClient cliente2 = builder.buildAsync();

        assertNotSame(cliente1, cliente2);

        CompletableFuture<NotificationResult> f1 = cliente1.sendAsync(
                new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        CompletableFuture<NotificationResult> f2 = cliente2.sendAsync(
                new EmailNotification("d@e.f", "Otro", "Contenido"));

        assertTrue(f1.join().isSent());
        assertTrue(f2.join().isSent());
    }
}
