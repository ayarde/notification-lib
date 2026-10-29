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

    private static void assertIdOpcio(String id, String canal, String destinatario) {
        assertNotNull(id);
        assertTrue(id.startsWith("sim-" + canal + "-"),
                "el identificador debe iniciar con sim-" + canal + "- pero fue: " + id);
        assertFalse(id.contains(destinatario),
                "el identificador no debe contener el destinatario pero fue: " + id);
    }

    @Test
    void envioEmailDespachaAlAdaptadorEmailYDevuelveResultado() {
        DefaultNotificationService servicio = servicioConTresAdaptadores();

        NotificationResult resultado = servicio.send(new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertIdOpcio(resultado.providerMessageId(), "email", "usuario@example.com");
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void envioSmsDespachaAlAdaptadorSmsYDevuelveResultado() {
        DefaultNotificationService servicio = servicioConTresAdaptadores();

        NotificationResult resultado = servicio.send(new SmsNotification("+5491112345678", "Mensaje de prueba"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertIdOpcio(resultado.providerMessageId(), "sms", "+5491112345678");
        assertNull(resultado.errorCode());
        assertNull(resultado.errorMessage());
    }

    @Test
    void envioPushDespachaAlAdaptadorPushYDevuelveResultado() {
        DefaultNotificationService servicio = servicioConTresAdaptadores();

        NotificationResult resultado = servicio.send(new PushNotification("token-dispositivo-123", "Título", "Cuerpo"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertIdOpcio(resultado.providerMessageId(), "push", "token-dispositivo-123");
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
        assertIdOpcio(emailResultado.providerMessageId(), "email", "a@b.c");

        assertTrue(smsResultado.isSent());
        assertIdOpcio(smsResultado.providerMessageId(), "sms", "+12345");

        assertTrue(pushResultado.isSent());
        assertIdOpcio(pushResultado.providerMessageId(), "push", "tok");
    }
}
