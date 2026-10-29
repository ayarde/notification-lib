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
 * <p>Si el ejecutor rechaza la tarea, {@link
 * java.util.concurrent.RejectedExecutionException} se propaga de forma
 * síncrona desde {@link #sendAsync(Notification)} antes de que se devuelva el
 * futuro.
 *
 * <p>Cancelar el {@link CompletableFuture} devuelto no detiene necesariamente
 * una operación de envío que ya comenzó en el adaptador.
 *
 * <p>El envío por lotes se implementa mediante {@link #sendBatchAsync(List)}.
 * Cada notificación del lote se envía de forma independiente usando el mismo
 * {@link Executor}. Los resultados individuales se ordenan según la posición
 * de la notificación de entrada, independientemente del orden de finalización
 * de las tareas.
 */
public class DefaultAsyncNotificationService implements AsyncNotificationClient {

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

        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.send(notification);
            } catch (NotificationValidationException | NotificationConfigurationException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new NotificationSendingException(
                        "Error inesperado al enviar la notificación de forma asíncrona", e);
            }
        }, executor);
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
                future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return delegate.send(notification);
                    } catch (NotificationValidationException | NotificationConfigurationException
                             | NotificationSendingException e) {
                        String mensaje = e.getMessage();
                        if (mensaje == null || mensaje.isBlank()) {
                            mensaje = "Error al enviar la notificación";
                        }
                        return NotificationResult.failed("ERROR_ENVIO", mensaje);
                    } catch (RuntimeException e) {
                        String mensaje = e.getMessage();
                        if (mensaje == null || mensaje.isBlank()) {
                            mensaje = "Error inesperado al enviar la notificación";
                        }
                        return NotificationResult.failed("ERROR_ENVIO", mensaje);
                    }
                }, executor);
            } catch (RejectedExecutionException e) {
                String mensaje = e.getMessage();
                if (mensaje == null || mensaje.isBlank()) {
                    mensaje = "El executor rechazó la tarea de envío";
                }
                future = CompletableFuture.completedFuture(
                        NotificationResult.failed("ERROR_EXECUTOR_RECHAZADO", mensaje));
            }
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
}
