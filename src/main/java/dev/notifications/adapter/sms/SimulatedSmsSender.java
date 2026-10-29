package dev.notifications.adapter.sms;

import dev.notifications.model.NotificationResult;
import dev.notifications.model.SmsNotification;
import dev.notifications.port.NotificationSender;

/**
 * Adaptador simulado para notificaciones SMS.
 *
 * <p>No realiza conexiones de red ni usa SDK de proveedores externos.
 * Devuelve un resultado {@code SENT} determinista con un identificador simulado
 * derivado del destinatario. No mantiene estado mutable ni modifica la notificación recibida.
 * <p>Este adaptador simulado siempre devuelve un resultado {@code SENT}.
 * Para probar escenarios {@code FAILED}, utilice un stub personalizado en lugar de este adaptador.
 */
public class SimulatedSmsSender implements NotificationSender<SmsNotification> {

    @Override
    public Class<SmsNotification> notificationType() {
        return SmsNotification.class;
    }

    @Override
    public NotificationResult send(SmsNotification notification) {
        return NotificationResult.sent("sim-sms-" + notification.recipient());
    }
}
