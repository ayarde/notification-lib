package dev.notifications.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de invariantes y fábricas de {@link NotificationResult}.
 */
class NotificationResultTest {

    @Test
    void resultadoSentNoContieneError() {
        NotificationResult r = NotificationResult.sent("msg-123");

        assertTrue(r.isSent());
        assertFalse(r.isFailed());
        assertEquals(NotificationStatus.SENT, r.status());
        assertEquals("msg-123", r.providerMessageId());
        assertNull(r.errorCode());
        assertNull(r.errorMessage());
    }

    @Test
    void resultadoSentSinIdDeProveedorEsValido() {
        NotificationResult r = NotificationResult.sent();

        assertTrue(r.isSent());
        assertFalse(r.isFailed());
        assertNull(r.providerMessageId());
        assertNull(r.errorCode());
        assertNull(r.errorMessage());
    }

    @Test
    void resultadoFailedContieneErrorYNoEsExitoso() {
        NotificationResult r = NotificationResult.failed("EC001", "No se pudo conectar con el proveedor");

        assertTrue(r.isFailed());
        assertFalse(r.isSent());
        assertEquals(NotificationStatus.FAILED, r.status());
        assertNull(r.providerMessageId());
        assertEquals("EC001", r.errorCode());
        assertEquals("No se pudo conectar con el proveedor", r.errorMessage());
    }

    @Test
    void resultadoSentConErrorCodeLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.SENT, "id", "EC001", null));
    }

    @Test
    void resultadoSentConErrorMessageLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.SENT, "id", null, "algo falló"));
    }

    @Test
    void resultadoFailedSinErrorCodeLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.FAILED, null, null, "msg"));
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.FAILED, null, "", "msg"));
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.FAILED, null, "   ", "msg"));
    }

    @Test
    void resultadoFailedSinErrorMessageLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.FAILED, null, "EC001", null));
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.FAILED, null, "EC001", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationResult(NotificationStatus.FAILED, null, "EC001", "   "));
    }

    @Test
    void estadoNuloLanzaExcepcion() {
        assertThrows(NullPointerException.class,
                () -> new NotificationResult(null, null, null, null));
    }
}
