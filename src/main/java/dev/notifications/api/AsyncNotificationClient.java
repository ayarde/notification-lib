package dev.notifications.api;

import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * API pública asíncrona de la librería de notificaciones.
 *
 * <p>Permite iniciar un envío sin bloquear al llamador durante toda la operación.
 * El resultado final se entrega mediante un {@link CompletableFuture}.
 *
 * <p>Esta API es independiente de la API síncrona {@link NotificationClient}.
 * Ambas pueden coexistir: el cliente síncrono sigue funcionando sin cambios.
 *
 * <p>Contrato de errores: los errores de validaci\u00f3n, configuraci\u00f3n y env\u00edo
 * no completan el futuro excepcionalmente. Se devuelve un
 * {@link NotificationResult} con estado {@code FAILED} cuyo
 * {@code errorCode} identifica el tipo de error ({@code ERROR_VALIDACION},
 * {@code ERROR_CONFIGURACION} o {@code ERROR_ENVIO}) y cuyo {@code errorMessage}
 * conserva el mensaje de la causa original. Un resultado {@code FAILED} completa
 * el futuro normalmente, sin lanzar excepci\u00f3n. Solo los errores no
 * capturados (por ejemplo, instancias de {@link Error}) pueden producir una
 * finalizaci\u00f3n excepcional del futuro.
 */
public interface AsyncNotificationClient {

    /**
     * Envía una notificación de forma asíncrona.
     *
     * <p>La operación se ejecuta en el ejecutor configurado. El llamador no
     * queda bloqueado esperando la respuesta del proveedor.
     *
     * @param notification notificación a enviar
     * @return futuro que se completa con el resultado del envío
     * @throws NullPointerException si {@code notification} es {@code null}
     */
    CompletableFuture<NotificationResult> sendAsync(Notification notification);

    /**
     * Envía una lista de notificaciones de forma asíncrona.
     *
     * <p>Cada notificación se envía de forma independiente usando el {@link Executor}
     * configurado por el consumidor. Los envíos pueden ejecutarse concurrentemente
     * de acuerdo con la capacidad y las políticas del ejecutor.
     *
     * <p>El orden de los resultados coincide con el orden de las notificaciones de
     * entrada. La posición {@code i} de la lista de resultados corresponde a la
     * posición {@code i} de la lista de notificaciones, aunque las tareas terminen
     * en otro orden.
     *
     * <p>El lote no es atómico. Un fallo individual no impide procesar las demás
     * notificaciones. Los resultados individuales pueden ser {@code SENT} o
     * {@code FAILED} de forma independiente.
     *
     * <p>Si la lista está vacía, se devuelve un futuro ya completado con una lista
     * vacía de resultados. Si la lista es nula o contiene al menos un elemento nulo,
     * se lanza {@link NullPointerException} de forma síncrona antes de programar
     * cualquier envío.
     *
     * <p>Si el {@link Executor} rechaza una tarea, la notificación correspondiente
     * obtiene un resultado {@code FAILED} con código de error
     * {@code ERROR_EXECUTOR_RECHAZADO} en su posición de la lista de resultados.
     * El lote continúa programando las notificaciones restantes y completa
     * normalmente con una lista de resultados mixtos ({@code SENT} y
     * {@code FAILED}) cuando todos los errores son capturados. Los errores no
     * capturados (por ejemplo, instancias de {@link Error}) pueden producir una
     * finalización excepcional del futuro del lote. Las tareas previamente
     * aceptadas por el executor pueden seguir ejecutándose y no se garantiza su
     * cancelación.
     *
     * <p>Cancelar el futuro del lote no detiene necesariamente las tareas de envío
     * que ya comenzaron en el adaptador.
     *
     * @param notifications lista de notificaciones a enviar
     * @return futuro que se completa con la lista de resultados en el mismo orden
     *         que las notificaciones de entrada
     * @throws NullPointerException si {@code notifications} es {@code null} o
     *         contiene al menos un elemento nulo
     */
    CompletableFuture<List<NotificationResult>> sendBatchAsync(List<Notification> notifications);
}
