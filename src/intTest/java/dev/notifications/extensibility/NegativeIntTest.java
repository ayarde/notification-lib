package dev.notifications.extensibility;

import dev.notifications.adapter.TestNotificationSender;
import dev.notifications.adapter.email.SimulatedEmailSender;
import dev.notifications.application.DefaultNotificationSenderRegistry;
import dev.notifications.application.DefaultNotificationService;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.exception.NotificationSendingException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.model.TestNotification;
import dev.notifications.port.NotificationSender;
import dev.notifications.validation.DefaultNotificationValidator;
import dev.notifications.validation.EmailNotificationValidator;
import dev.notifications.validation.TestNotificationValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas negativas de integración: configuraciones incompatibles, proveedores ausentes,
 * validaciones fallidas antes del envío y excepciones inesperadas del adaptador.
 */
class NegativeIntTest {

    @Test
    void proveedorPredeterminadoInexistenteLanzaExcepcionDeConfiguracion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();

        assertThrows(NotificationConfigurationException.class,
                () -> registry.setDefaultProvider(EmailNotification.class, "no-existe"));
    }

    @Test
    void tipoSinProveedorRegistradoLanzaExcepcionDeConfiguracion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();

        assertThrows(NotificationConfigurationException.class,
                () -> registry.setDefaultProvider(TestNotification.class, "test-primary"));
    }

    @Test
    void validacionFallidaImpideInvocacionAlAdaptador() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        boolean[] adaptadorInvocado = {false};
        registry.register("test-primary", TestNotification.class, new NotificationSender<TestNotification>() {
            @Override
            public Class<TestNotification> notificationType() {
                return TestNotification.class;
            }

            @Override
            public NotificationResult send(TestNotification notification) {
                adaptadorInvocado[0] = true;
                return NotificationResult.sent("msg");
            }
        });
        registry.setDefaultProvider(TestNotification.class, "test-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new TestNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        assertThrows(Exception.class,
                () -> servicio.send(new TestNotification("")));

        assertFalse(adaptadorInvocado[0]);
    }

    @Test
    void excepcionInesperadaDelAdaptadorSePropagaSinConvertirEnFailed() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("test-primary", TestNotification.class, new NotificationSender<TestNotification>() {
            @Override
            public Class<TestNotification> notificationType() {
                return TestNotification.class;
            }

            @Override
            public NotificationResult send(TestNotification notification) {
                throw new IllegalStateException("error inesperado");
            }
        });
        registry.setDefaultProvider(TestNotification.class, "test-primary");

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new TestNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        NotificationSendingException excepcion = assertThrows(NotificationSendingException.class,
                () -> servicio.send(new TestNotification("valido")));

        assertSame(IllegalStateException.class, excepcion.getCause().getClass());
        assertEquals("error inesperado", excepcion.getCause().getMessage());
    }

    @Test
    void envioSinDefaultConfiguradoLanzaExcepcionDeConfiguracion() {
        DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
        registry.register("email-primary", EmailNotification.class, new SimulatedEmailSender());
        // No se configura default para email

        DefaultNotificationValidator validator = new DefaultNotificationValidator();
        validator.register(new EmailNotificationValidator());

        DefaultNotificationService servicio = new DefaultNotificationService(validator, registry);

        assertThrows(NotificationConfigurationException.class,
                () -> servicio.send(new EmailNotification("a@b.c", "Asunto", "Cuerpo")));
    }
}
