package dev.notifications.adapter;

import dev.notifications.application.DefaultNotificationSenderRegistry;
import dev.notifications.application.DefaultNotificationService;
import dev.notifications.adapter.email.SimulatedEmailSender;
import dev.notifications.adapter.push.SimulatedPushSender;
import dev.notifications.adapter.sms.SimulatedSmsSender;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.validation.DefaultNotificationValidator;
import dev.notifications.validation.EmailNotificationValidator;
import dev.notifications.validation.PushNotificationValidator;
import dev.notifications.validation.SmsNotificationValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de los adaptadores simulados con el registro y el servicio.
 *
 * <p>Registra los tres adaptadores, configura proveedores predeterminados y envía
 * una notificación de cada tipo mediante {@link DefaultNotificationService}.
 */
class SimulatedAdaptersIntTest {

    private DefaultNotificationService servicioConTresAdaptadores() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class, new SimulatedEmailSender());
        registry.register("sms-primary", SmsNotification.class, new SimulatedSmsSender());
        registry.register("push-primary", PushNotification.class, new SimulatedPushSender());
        registry.setDefaultProvider(EmailNotification.class, "email-primary");
        registry.setDefaultProvider(SmsNotification.class, "sms-primary");
        registry.setDefaultProvider(PushNotification.class, "push-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());
        validator.register(new SmsNotificationValidator());
        validator.register(new PushNotificationValidator());

        return new DefaultNotificationService(validator, registry);
    }

    @Test
    void envioEmailDespachaAlAdaptadorEmailYDevuelveResultado() {
        DefaultNotificationService servicio = servicioConTresAdaptadores();

        NotificationResult resultado = servicio.send(new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals("sim-email-usuario@example.com", resultado.providerMessageId());
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void envioSmsDespachaAlAdaptadorSmsYDevuelveResultado() {
        DefaultNotificationService servicio = servicioConTresAdaptadores();

        NotificationResult resultado = servicio.send(new SmsNotification("+5491112345678", "Mensaje de prueba"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals("sim-sms-+5491112345678", resultado.providerMessageId());
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void envioPushDespachaAlAdaptadorPushYDevuelveResultado() {
        DefaultNotificationService servicio = servicioConTresAdaptadores();

        NotificationResult resultado = servicio.send(new PushNotification("token-dispositivo-123", "Título", "Cuerpo"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertEquals("sim-push-token-dispositivo-123", resultado.providerMessageId());
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void cadaCanalDespachaAlProveedorCorrespondienteYNoAlOtro() {
        DefaultNotificationService servicio = servicioConTresAdaptadores();

        NotificationResult emailResultado = servicio.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo"));
        NotificationResult smsResultado = servicio.send(new SmsNotification("+12345", "Mensaje"));
        NotificationResult pushResultado = servicio.send(new PushNotification("tok", "Título", "Cuerpo"));

        assertTrue(emailResultado.isSent());
        assertEquals("sim-email-a@b.c", emailResultado.providerMessageId());

        assertTrue(smsResultado.isSent());
        assertEquals("sim-sms-+12345", smsResultado.providerMessageId());

        assertTrue(pushResultado.isSent());
        assertEquals("sim-push-tok", pushResultado.providerMessageId());
    }
}
