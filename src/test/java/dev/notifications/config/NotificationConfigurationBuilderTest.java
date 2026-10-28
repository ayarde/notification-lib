package dev.notifications.config;

import dev.notifications.adapter.email.SimulatedEmailSender;
import dev.notifications.adapter.push.SimulatedPushSender;
import dev.notifications.adapter.sms.SimulatedSmsSender;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.api.NotificationClient;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import org.junit.jupiter.api.Test;

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
}
