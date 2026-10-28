package dev.notifications.port;

import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;

/**
 * Puerto de salida y Strategy de envío.
 *
 * <p>Cada adaptador declara exactamente el tipo de notificación que acepta mediante
 * {@link #notificationType()}. El registro de adaptadores utiliza esta información para
 * despachar de forma type-safe sin casts genéricos inseguros.
 *
 * @param <N> tipo de notificación que este remitente acepta
 */
public interface NotificationSender<N extends Notification> {

    Class<N> notificationType();

    NotificationResult send(N notification);
}
