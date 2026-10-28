package dev.notifications.application;

import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.NotificationStatus;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.port.NotificationSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del registro de adaptadores con múltiples proveedores por providerId.
 */
class DefaultNotificationSenderRegistryTest {

    private NotificationSender<EmailNotification> emailSender(String providerId,
                                                              NotificationResult resultado) {
        return new NotificationSender<EmailNotification>() {
            @Override
            public Class<EmailNotification> notificationType() {
                return EmailNotification.class;
            }

            @Override
            public NotificationResult send(EmailNotification notification) {
                return resultado;
            }
        };
    }

    private NotificationSender<Notification> notificationSender(NotificationResult resultado) {
        return new NotificationSender<Notification>() {
            @Override
            public Class<Notification> notificationType() {
                return Notification.class;
            }

            @Override
            public NotificationResult send(Notification notification) {
                return resultado;
            }
        };
    }

    @Test
    void registraVariosProveedoresParaElMismoTipo() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                emailSender("email-primary", NotificationResult.sent("msg-1")));
        registry.register("email-backup", EmailNotification.class,
                emailSender("email-backup", NotificationResult.sent("msg-2")));

        assertTrue(registry.hasProvider("email-primary"));
        assertTrue(registry.hasProvider("email-backup"));
    }

    @Test
    void rechazaProviderIdDuplicado() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                emailSender("email-primary", NotificationResult.sent("msg-1")));

        assertThrows(NotificationConfigurationException.class,
                () -> registry.register("email-primary", EmailNotification.class,
                        emailSender("email-primary", NotificationResult.sent("msg-2"))));
    }

    @Test
    void rechazaProveedorIncompatibleConElTipoDeclarado() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();

        assertThrows(NotificationConfigurationException.class,
                () -> registry.register("email-primary", SmsNotification.class,
                        emailSender("email-primary", NotificationResult.sent("msg-1"))));
    }

    @Test
    void setDefaultProviderAlmacenaProveedorPorTipo() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                emailSender("email-primary", NotificationResult.sent("msg-1")));
        registry.setDefaultProvider(EmailNotification.class, "email-primary");

        assertTrue(registry.hasDefaultProvider(EmailNotification.class));
        assertEquals("email-primary", registry.getDefaultProvider(EmailNotification.class));
    }

    @Test
    void setDefaultProviderConProviderIdInexistenteLanzaExcepcion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();

        assertThrows(NotificationConfigurationException.class,
                () -> registry.setDefaultProvider(EmailNotification.class, "no-existe"));
    }

    @Test
    void setDefaultProviderConProveedorIncompatibleLanzaExcepcion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("sms-primary", SmsNotification.class,
                new NotificationSender<SmsNotification>() {
                    @Override
                    public Class<SmsNotification> notificationType() {
                        return SmsNotification.class;
                    }

                    @Override
                    public NotificationResult send(SmsNotification notification) {
                        return NotificationResult.sent("msg-1");
                    }
                });

        assertThrows(NotificationConfigurationException.class,
                () -> registry.setDefaultProvider(EmailNotification.class, "sms-primary"));
    }

    @Test
    void despachaAlProveedorPredeterminadoSegunElTipo() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        NotificationResult resultadoEsperado = NotificationResult.sent("msg-1");
        registry.register("email-primary", EmailNotification.class,
                emailSender("email-primary", resultadoEsperado));
        registry.setDefaultProvider(EmailNotification.class, "email-primary");

        NotificationResult resultado = registry.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertEquals("msg-1", resultado.providerMessageId());
        assertTrue(resultado.isSent());
    }

    @Test
    void envioSinProveedorPredeterminadoLanzaExcepcionDeConfiguracion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();

        assertThrows(NotificationConfigurationException.class,
                () -> registry.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));
    }

    @Test
    void proveedorPredeterminadoIncompatibleConLaNotificacionLanzaExcepcionEnEnvio() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("sms-primary", SmsNotification.class,
                new NotificationSender<SmsNotification>() {
                    @Override
                    public Class<SmsNotification> notificationType() {
                        return SmsNotification.class;
                    }

                    @Override
                    public NotificationResult send(SmsNotification notification) {
                        return NotificationResult.sent("msg-1");
                    }
                });
        registry.setDefaultProvider(SmsNotification.class, "sms-primary");

        assertThrows(NotificationConfigurationException.class,
                () -> registry.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));
    }
}
