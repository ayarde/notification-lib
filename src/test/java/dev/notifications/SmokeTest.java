package dev.notifications;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de humo para verificar que el entorno de pruebas (Maven + JUnit 5)
 * está configurado correctamente y ejecuta tests sin dependencias externas.
 */
class SmokeTest {

    @Test
    void elEntornoDePruebasFunciona() {
        assertTrue(true, "El entorno de pruebas está operativo");
    }
}
