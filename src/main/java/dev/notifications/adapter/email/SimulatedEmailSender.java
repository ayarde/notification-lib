package dev.notifications.adapter.email;

import dev.notifications.model.EmailNotification;
import dev.notifications.model.NotificationResult;
import dev.notifications.port.NotificationSender;

/**
 * Adaptador simulado para notificaciones Email.
 *
 * <p>No realiza conexiones de red ni usa SDK de proveedores externos.
 * Devuelve un resultado {@code SENT} determinista con un identificador simulado
 * derivado del destinatario. No mantiene estado mutable ni modifica la notificación recibida.
 * <p>Este adaptador simulado siempre devuelve un resultado {@code SENT}.
 * Para probar escenarios {@code FAILED}, utilice un stub personalizado en lugar de este adaptador.
 */
public class SimulatedEmailSender implements NotificationSender<EmailNotification> {

    @Override
    public Class<EmailNotification> notificationType() {
        return EmailNotification.class;
    }

    @Override
    public NotificationResult send(EmailNotification notification) {
        return NotificationResult.sent("sim-email-" + notification.recipient());
    }
}
