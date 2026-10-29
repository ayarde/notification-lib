package dev.notifications.config;

import dev.notifications.adapter.email.SimulatedEmailSender;
import dev.notifications.adapter.push.SimulatedPushSender;
import dev.notifications.adapter.sms.SimulatedSmsSender;
import dev.notifications.api.NotificationClient;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de {@link NotificationConfiguration} con los tres adaptadores simulados.
 *
 * <p>Verifica envíos exitosos, selección correcta de proveedor y rechazo de notificaciones
 * inválidas antes del envío.
 */
class NotificationConfigurationIntTest {

    private static void assertIdOpcio(String id, String canal, String destinatario) {
        assertNotNull(id);
        assertTrue(id.startsWith("sim-" + canal + "-"),
                "el identificador debe iniciar con sim-" + canal + "- pero fue: " + id);
        assertFalse(id.contains(destinatario),
                "el identificador no debe contener el destinatario pero fue: " + id);
    }

    @Test
    void envioEmailValidoDevuelveResultadoSent() {
        NotificationConfigTestHelper helper = new NotificationConfigTestHelper();
        NotificationClient client = helper.build();

        NotificationResult resultado = client.send(new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertIdOpcio(resultado.providerMessageId(), "email", "usuario@example.com");
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void envioSmsValidoDevuelveResultadoSent() {
        NotificationConfigTestHelper helper = new NotificationConfigTestHelper();
        NotificationClient client = helper.build();

        NotificationResult resultado = client.send(new SmsNotification("+5491112345678", "Mensaje de prueba"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertIdOpcio(resultado.providerMessageId(), "sms", "+5491112345678");
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void envioPushValidoDevuelveResultadoSent() {
        NotificationConfigTestHelper helper = new NotificationConfigTestHelper();
        NotificationClient client = helper.build();

        NotificationResult resultado = client.send(new PushNotification("token-dispositivo-123", "Título", "Cuerpo"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertIdOpcio(resultado.providerMessageId(), "push", "token-dispositivo-123");
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void envioEmailInvalidoEsRechazadoAntesDelEnvio() {
        NotificationConfigTestHelper helper = new NotificationConfigTestHelper();
        NotificationClient client = helper.build();

        assertThrows(Exception.class,
                () -> client.send(new EmailNotification("", "Asunto", "Cuerpo")));
    }

    @Test
    void envioSmsInvalidoEsRechazadoAntesDelEnvio() {
        NotificationConfigTestHelper helper = new NotificationConfigTestHelper();
        NotificationClient client = helper.build();

        assertThrows(Exception.class,
                () -> client.send(new SmsNotification("", "Mensaje")));
    }

    @Test
    void envioPushInvalidoEsRechazadoAntesDelEnvio() {
        NotificationConfigTestHelper helper = new NotificationConfigTestHelper();
        NotificationClient client = helper.build();

        assertThrows(Exception.class,
                () -> client.send(new PushNotification("", "Título", "Cuerpo")));
    }

    /**
     * Helper para construir un cliente configurado con los tres adaptadores simulados.
     */
    static class NotificationConfigTestHelper {

        private final NotificationConfiguration.Builder builder;

        NotificationConfigTestHelper() {
            builder = NotificationConfiguration.builder()
                    .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
                    .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
                    .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
                    .defaultProvider(EmailNotification.class, "email-primary")
                    .defaultProvider(SmsNotification.class, "sms-primary")
                    .defaultProvider(PushNotification.class, "push-primary");
        }

        dev.notifications.api.NotificationClient build() {
            return builder.build();
        }
    }
}
