package dev.notifications.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las excepciones documentadas: construcción con mensaje, preservación de causa
 * y jerarquía RuntimeException.
 */
class NotificationExceptionsTest {

    @Test
    void validacionExceptionConMensaje() {
        NotificationValidationException e = new NotificationValidationException("Email inválido");

        assertEquals("Email inválido", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void validacionExceptionConservaCausa() {
        Exception causa = new IllegalStateException("subyacente");
        NotificationValidationException e = new NotificationValidationException("fallo de validación", causa);

        assertEquals("fallo de validación", e.getMessage());
        assertSame(causa, e.getCause());
    }

    @Test
    void configuracionExceptionConMensaje() {
        NotificationConfigurationException e = new NotificationConfigurationException("Proveedor no encontrado");

        assertEquals("Proveedor no encontrado", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void configuracionExceptionConservaCausa() {
        Exception causa = new IllegalArgumentException("registro duplicado");
        NotificationConfigurationException e = new NotificationConfigurationException("config inválida", causa);

        assertEquals("config inválida", e.getMessage());
        assertSame(causa, e.getCause());
    }

    @Test
    void envioExceptionConMensajeYCausa() {
        Exception causa = new IllegalStateException("red no disponible");
        NotificationSendingException e = new NotificationSendingException("error de envío", causa);

        assertEquals("error de envío", e.getMessage());
        assertSame(causa, e.getCause());
    }

    @Test
    void todasSonRuntimeException() {
        assertInstanceOf(RuntimeException.class, new NotificationValidationException("x"));
        assertInstanceOf(RuntimeException.class, new NotificationConfigurationException("x"));
        assertInstanceOf(RuntimeException.class, new NotificationSendingException("x"));
    }
}
