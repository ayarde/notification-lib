package dev.notifications.extensibility;

import dev.notifications.adapter.TestNotificationSender;
import dev.notifications.application.DefaultNotificationSenderRegistry;
import dev.notifications.application.DefaultNotificationService;
import dev.notifications.model.TestNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.validation.DefaultNotificationValidator;
import dev.notifications.validation.TestNotificationValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de extensibilidad: demuestra que un canal de prueba nuevo puede registrarse
 * y despacharse mediante las abstracciones existentes sin modificar
 * DefaultNotificationService, DefaultNotificationSenderRegistry ni agregar un switch
 * por tipo.
 */
class ExtensibilityIntTest {

    @Test
    void testNotificationPuedeRegistrarseYDespacharseSinModificarProduccion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("test-primary", TestNotification.class, new TestNotificationSender());
        registry.setDefaultProvider(TestNotification.class, "test-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new TestNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        NotificationResult resultado = servicio.send(new TestNotification("destinatario-prueba"));

        assertTrue(resultado.isSent());
        assertFalse(resultado.isFailed());
        assertNotNull(resultado.providerMessageId());
        assertTrue(resultado.providerMessageId().startsWith("sim-test-"));
        assertFalse(resultado.providerMessageId().contains("destinatario-prueba"));
    }

    @Test
    void testNotificationInvalidaEsRechazadaAntesDelEnvio() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("test-primary", TestNotification.class, new TestNotificationSender());
        registry.setDefaultProvider(TestNotification.class, "test-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new TestNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        assertThrows(Exception.class,
                () -> servicio.send(new TestNotification("")));
    }
}
