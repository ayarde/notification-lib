package dev.notifications.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de construcción de los modelos de notificación y sus diferencias entre canales.
 */
class NotificationModelTest {

    @Test
    void emailNotificationConservaDestinatarioAsuntoYCuerpo() {
        EmailNotification n = new EmailNotification("usuario@example.com", "Asunto", "Cuerpo del email");

        assertEquals("usuario@example.com", n.recipient());
        assertEquals("Asunto", n.subject());
        assertEquals("Cuerpo del email", n.body());
    }

    @Test
    void smsNotificationConservaDestinatarioYMensajeSinExigirAsunto() {
        SmsNotification n = new SmsNotification("+5491112345678", "Hola, esta es una prueba de SMS");

        assertEquals("+5491112345678", n.recipient());
        assertEquals("Hola, esta es una prueba de SMS", n.message());
    }

    @Test
    void pushNotificationConservaDestinoTituloYCuerpo() {
        PushNotification n = new PushNotification("token-dispositivo-123", "Título push", "Cuerpo de la notificación");

        assertEquals("token-dispositivo-123", n.recipient());
        assertEquals("Título push", n.title());
        assertEquals("Cuerpo de la notificación", n.body());
    }

    @Test
    void todosLosModelosImplementanNotification() {
        assertInstanceOf(Notification.class,
                new EmailNotification("a@b.c", "s", "b"));
        assertInstanceOf(Notification.class,
                new SmsNotification("+12345", "m"));
        assertInstanceOf(Notification.class,
                new PushNotification("tok", "t", "b"));
    }

    @Test
    void emailNotificationSinCamposObligatoriosEsRechazadoPorElValidadorEnEtapaPosterior() {
        // Este test documenta que el modelo acepta construir el objeto; la validación de
        // contenido (campos no vacíos, formatos) corresponde al componente de validación
        // de la Etapa 3, no al modelo en sí.
        EmailNotification n = new EmailNotification("", "", "");
        assertNotNull(n);
        assertEquals("", n.recipient());
        assertEquals("", n.subject());
        assertEquals("", n.body());
    }
}
