package dev.notifications.application;

import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.exception.NotificationSendingException;
import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.SmsNotification;
import dev.notifications.port.NotificationSender;
import dev.notifications.validation.DefaultNotificationValidator;
import dev.notifications.validation.EmailNotificationValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del servicio de aplicación: flujo completo, validación antes del envío,
 * resultados fallidos y excepciones inesperadas del adaptador.
 */
class DefaultNotificationServiceTest {

    private DefaultNotificationService servicioConAdaptador(NotificationResult resultado) {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                new FixedResultNotificationSender(resultado));
        registry.setDefaultProvider(EmailNotification.class, "email-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        return new DefaultNotificationService(validator, registry);
    }

    @Test
    void envioValidoDevuelveResultadoDelAdaptador() {
        NotificationResult esperado = NotificationResult.sent("msg-123");
        DefaultNotificationService servicio = servicioConAdaptador(esperado);

        NotificationResult resultado = servicio.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertEquals(esperado, resultado);
        assertTrue(resultado.isSent());
        assertEquals("msg-123", resultado.providerMessageId());
    }

    @Test
    void adaptadorDevuelveFailedSinConvertirEnExcepcion() {
        NotificationResult fallido = NotificationResult.failed("EC001", "Proveedor no disponible");
        DefaultNotificationService servicio = servicioConAdaptador(fallido);

        NotificationResult resultado = servicio.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));

        assertSame(fallido, resultado);
        assertTrue(resultado.isFailed());
        assertEquals("EC001", resultado.errorCode());
        assertEquals("Proveedor no disponible", resultado.errorMessage());
    }

    @Test
    void excepcionInesperadaDelAdaptadorSePropagaComoNotificationSendingException() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                new NotificationSender<EmailNotification>() {
                    @Override
                    public Class<EmailNotification> notificationType() {
                        return EmailNotification.class;
                    }

                    @Override
                    public NotificationResult send(EmailNotification notification) {
                        throw new IllegalStateException("red caida");
                    }
                });
        registry.setDefaultProvider(EmailNotification.class, "email-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        NotificationSendingException excepcion = assertThrows(NotificationSendingException.class,
                () -> servicio.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));

        assertSame(IllegalStateException.class, excepcion.getCause().getClass());
        assertEquals("red caida", excepcion.getCause().getMessage());
    }

    @Test
    void excepcionInesperadaDelAdaptadorPreservaCausaOriginal() {
        RuntimeException causa = new IllegalStateException("fallo de red");
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                new NotificationSender<EmailNotification>() {
                    @Override
                    public Class<EmailNotification> notificationType() {
                        return EmailNotification.class;
                    }

                    @Override
                    public NotificationResult send(EmailNotification notification) {
                        throw causa;
                    }
                });
        registry.setDefaultProvider(EmailNotification.class, "email-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        NotificationSendingException excepcion = assertThrows(NotificationSendingException.class,
                () -> servicio.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));

        assertSame(causa, excepcion.getCause());
    }

    @Test
    void notificacionInvalidaEsRechazadaAntesDeInvocarAlAdaptador() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        boolean[] adaptadorInvocado = {false};
        registry.register("email-primary", EmailNotification.class,
                new NotificationSender<EmailNotification>() {
                    @Override
                    public Class<EmailNotification> notificationType() {
                        return EmailNotification.class;
                    }

                    @Override
                    public NotificationResult send(EmailNotification notification) {
                        adaptadorInvocado[0] = true;
                        return NotificationResult.sent("msg");
                    }
                });
        registry.setDefaultProvider(EmailNotification.class, "email-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        assertThrows(NotificationValidationException.class,
                () -> servicio.send(new EmailNotification("", "Asunto", "Cuerpo")));

        assertFalse(adaptadorInvocado[0]);
    }

    @Test
    void proveedorPredeterminadoInexistenteGeneraErrorDeConfiguracion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                new FixedResultNotificationSender(NotificationResult.sent()));
        // No se llama a setDefaultProvider; no hay default configurado.

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        assertThrows(NotificationConfigurationException.class,
                () -> servicio.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));
    }

    @Test
    void proveedorPredeterminadoIncompatibleGeneraErrorDeConfiguracion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("sms-primary", SmsNotification.class,
                new FixedResultNotificationSender(NotificationResult.sent()));

        assertThrows(NotificationConfigurationException.class,
                () -> registry.setDefaultProvider(EmailNotification.class, "sms-primary"));
    }

    @Test
    void notificacionNulaLanzaExcepcion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class,
                new FixedResultNotificationSender(NotificationResult.sent()));
        registry.setDefaultProvider(EmailNotification.class, "email-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        assertThrows(NullPointerException.class, () -> servicio.send(null));
    }
}
