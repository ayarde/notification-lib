package dev.notifications.application;

import dev.notifications.api.AsyncNotificationClient;
import dev.notifications.api.NotificationClient;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.exception.NotificationSendingException;
import dev.notifications.exception.NotificationValidationException;
import dev.notifications.model.Notification;
import dev.notifications.model.NotificationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Implementación predeterminada del cliente asíncrono de notificaciones.
 *
 * <p>Envuelve un {@link NotificationClient} y delega cada envío en una tarea
 * ejecutada por el {@link Executor} proporcionado. El resultado se entrega
 * mediante un {@link CompletableFuture}.
 *
 * <p>Este servicio no crea ni cierra el ejecutor: el consumidor es responsable
 * de su ciclo de vida. El constructor lanza {@link NullPointerException} si
 * el ejecutor es {@code null}; {@link
 * dev.notifications.config.NotificationConfiguration.Builder#buildAsync()}
 * lanza {@link IllegalStateException} si no se configuró un ejecutor.
 *
 * <p>La composición con reintentos se resuelve en el delegate: si el
 * {@link NotificationClient} envuelto es un {@link
 * dev.notifications.retry.RetryingNotificationClient}, los reintentos se
 * ejecutan en el hilo del ejecutor, sin bloquear al llamador.
 *
 * <p>Contrato de errores (coherente entre {@link #sendAsync} y
 * {@link #sendBatchAsync}): los errores de validación, configuración y envío
 * no completan el futuro excepcionalmente. Se devuelve un
 * {@link NotificationResult} con estado {@code FAILED} cuyo
 * {@code errorCode} identifica el tipo de error y cuyo {@code errorMessage}
 * conserva el mensaje de la causa original:
 * <ul>
 *   <li>{@code ERROR_VALIDACION}: {@link NotificationValidationException}.</li>
 *   <li>{@code ERROR_CONFIGURACION}: {@link NotificationConfigurationException}.</li>
 *   <li>{@code ERROR_ENVIO}: {@link NotificationSendingException} u otra
 *       excepción inesperada de tiempo de ejecución.</li>
 * </ul>
 * Solo los errores no capturados (por ejemplo, instancias de {@link Error})
 * pueden producir una finalización excepcional del futuro.
 *
 * <p>Si el ejecutor rechaza la tarea, {@link
 * java.util.concurrent.RejectedExecutionException} se propaga de forma
 * síncrona desde {@link #sendAsync(Notification)} antes de que se devuelva el
 * futuro. En {@link #sendBatchAsync(List)} la notificación correspondiente
 * obtiene un resultado {@code FAILED} con código
 * {@code ERROR_EXECUTOR_RECHAZADO} y el lote continúa con las restantes.
 *
 * <p>Cancelar el {@link CompletableFuture} devuelto no detiene necesariamente
 * una operación de envío que ya comenzó en el adaptador.
 *
 * <p>El envío por lotes se implementa mediante {@link #sendBatchAsync(List)}.
 * Cada notificación del lote se envía de forma independiente usando el mismo
 * {@link Executor}. Los resultados individuales se ordenan según la posición
 * de la notificación de entrada, independientemente del orden de finalización
 * de las tareas. El lote no es atómico: un fallo individual no impide
 * procesar las demás notificaciones.
 */
public class DefaultAsyncNotificationService implements AsyncNotificationClient {

    private static final String ERROR_VALIDACION = "ERROR_VALIDACION";
    private static final String ERROR_CONFIGURACION = "ERROR_CONFIGURACION";
    private static final String ERROR_ENVIO = "ERROR_ENVIO";
    private static final String ERROR_EXECUTOR_RECHAZADO = "ERROR_EXECUTOR_RECHAZADO";

    private final NotificationClient delegate;
    private final Executor executor;

    /**
     * Crea un cliente asíncrono.
     *
     * @param delegate cliente síncrono a envolver (puede incluir reintentos)
     * @param executor ejecutor donde se ejecutan las tareas de envío
     * @throws NullPointerException si alguno de los argumentos es {@code null}
     */
    public DefaultAsyncNotificationService(NotificationClient delegate, Executor executor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate no puede ser nulo");
        this.executor = Objects.requireNonNull(executor, "executor no puede ser nulo");
    }

    @Override
    public CompletableFuture<NotificationResult> sendAsync(Notification notification) {
        Objects.requireNonNull(notification, "notification no puede ser nula");

        return CompletableFuture.supplyAsync(() -> enviarResultado(notification), executor);
    }

    /**
     * Envía una notificación y devuelve un resultado, convirtiendo cualquier
     * error controlado en un resultado {@code FAILED} sin completar el futuro
     * excepcionalmente.
     */
    private NotificationResult enviarResultado(Notification notification) {
        try {
            return delegate.send(notification);
        } catch (NotificationValidationException e) {
            return fallido(ERROR_VALIDACION, e);
        } catch (NotificationConfigurationException e) {
            return fallido(ERROR_CONFIGURACION, e);
        } catch (NotificationSendingException e) {
            return fallido(ERROR_ENVIO, e);
        } catch (RuntimeException e) {
            return fallido(ERROR_ENVIO, e);
        }
    }

    private static NotificationResult fallido(String codigo, Throwable causa) {
        String mensaje = causa.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = "Error al enviar la notificación";
        }
        return NotificationResult.failed(codigo, mensaje);
    }

    @Override
    public CompletableFuture<List<NotificationResult>> sendBatchAsync(List<Notification> notifications) {
        Objects.requireNonNull(notifications, "notifications no puede ser nula");

        // Validar toda la entrada antes de programar cualquier tarea
        for (Notification notification : notifications) {
            Objects.requireNonNull(notification, "una notificación de la lista no puede ser nula");
        }

        if (notifications.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<NotificationResult>> futures = new ArrayList<>(notifications.size());

        for (Notification notification : notifications) {
            CompletableFuture<NotificationResult> future;
            try {
                future = CompletableFuture.supplyAsync(() -> enviarResultado(notification), executor);
            } catch (RejectedExecutionException e) {
                String mensaje = e.getMessage();
                if (mensaje == null || mensaje.isBlank()) {
                    mensaje = "El executor rechazó la tarea de envío";
                }
                future = CompletableFuture.completedFuture(
                        NotificationResult.failed(ERROR_EXECUTOR_RECHAZADO, mensaje));
            }
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
}
