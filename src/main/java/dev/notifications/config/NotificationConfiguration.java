package dev.notifications.config;

import dev.notifications.api.AsyncNotificationClient;
import dev.notifications.api.NotificationClient;
import dev.notifications.application.DefaultAsyncNotificationService;
import dev.notifications.application.DefaultNotificationService;
import dev.notifications.application.DefaultNotificationSenderRegistry;
import dev.notifications.exception.NotificationConfigurationException;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.Notification;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.port.NotificationSender;
import dev.notifications.retry.RetryPolicy;
import dev.notifications.retry.RetryingNotificationClient;
import dev.notifications.validation.DefaultNotificationValidator;
import dev.notifications.validation.EmailNotificationValidator;
import dev.notifications.validation.PushNotificationValidator;
import dev.notifications.validation.SmsNotificationValidator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Configuración de la librería de notificaciones construida mediante código Java.
 *
 * <p>Permite registrar adaptadores de envío con identificadores únicos ({@code providerId}),
 * definir proveedores predeterminados por tipo de notificación y obtener un cliente listo
 * para enviar mediante {@link #build()}.
 *
 * <p>Cada llamada a {@code build()} crea instancias nuevas de registro, validador y servicio,
 * por lo que los clientes construidos están aislados entre sí y no se ven afectados por
 * modificaciones posteriores del Builder.
 */
public class NotificationConfiguration {

    private NotificationConfiguration() {
    }

    /**
     * Crea un nuevo Builder para construir una configuración de notificaciones.
     *
     * @return una nueva instancia de Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder de {@link NotificationConfiguration}.
     *
     * <p>Acumula los proveedores y defaults configurados. Cada llamada a
     * {@link #build()} crea un registro, un validador y un servicio nuevos, aislados de
     * clientes construidos previamente.
     */
    public static class Builder {

        private static class ProviderEntry {
            final Class<? extends Notification> type;
            final NotificationSender<?> sender;

            ProviderEntry(Class<? extends Notification> type, NotificationSender<?> sender) {
                this.type = type;
                this.sender = sender;
            }
        }

        private final Map<String, ProviderEntry> providersById = new HashMap<>();
        private final Map<Class<? extends Notification>, String> defaultProviderByType = new HashMap<>();
        private final Set<String> registeredProviderIds = new HashSet<>();
        private RetryPolicy retryPolicy;
        private Executor executor;

        private Builder() {
        }

        /**
         * Registra un proveedor de envío para un tipo de notificación.
         *
         * @param providerId identificador único del proveedor
         * @param type       tipo de notificación que el proveedor acepta
         * @param sender     adaptador a registrar
         * @return este Builder
         * @throws NotificationConfigurationException si el providerId ya fue registrado
         *                                            o el proveedor no es compatible con el type
         */
        public Builder addSender(String providerId,
                                 Class<? extends Notification> type,
                                 NotificationSender<?> sender) {
            Objects.requireNonNull(providerId, "providerId no puede ser nulo");
            Objects.requireNonNull(type, "type no puede ser nulo");
            Objects.requireNonNull(sender, "sender no puede ser nulo");

            if (!registeredProviderIds.add(providerId)) {
                throw new NotificationConfigurationException(
                        "Ya existe un proveedor registrado con el identificador: " + providerId);
            }

            providersById.put(providerId, new ProviderEntry(type, sender));
            return this;
        }

        /**
         * Define una política de reintentos para el cliente construido.
         *
         * <p>Cuando se establece una política, el cliente devuelto por
         * {@link #build()} reintentará el envío automáticamente cuando el
         * resultado sea {@code FAILED} según la política configurada.
         * Cuando no se establece ninguna política, el cliente envía una sola vez
         * sin reintentos (comportamiento predeterminado).
         *
         * @param retryPolicy política de reintentos
         * @return este Builder
         */
        public Builder withRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * Define el ejecutor donde se ejecutarán los envíos asíncronos.
         *
         * <p>El consumidor es responsable del ciclo de vida del ejecutor:
         * la librería no lo crea ni lo cierra. Si no se configura ningún
         * ejecutor, {@link #buildAsync()} lanza {@link IllegalStateException}.
         *
         * @param executor ejecutor para tareas de envío asíncrono
         * @return este Builder
         */
        public Builder withExecutor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor no puede ser nulo");
            return this;
        }

        /**
         * Define el proveedor predeterminado para un tipo de notificación.
         *
         * @param type        tipo de notificación
         * @param providerId  identificador único del proveedor ya registrado
         * @return este Builder
         * @throws NotificationConfigurationException si el providerId no existe
         *                                            o no es compatible con el type
         */
        public Builder defaultProvider(Class<? extends Notification> type,
                                       String providerId) {
            Objects.requireNonNull(type, "type no puede ser nulo");
            Objects.requireNonNull(providerId, "providerId no puede ser nulo");

            ProviderEntry entry = providersById.get(providerId);
            if (entry == null) {
                throw new NotificationConfigurationException(
                        "No existe un proveedor registrado con el identificador: " + providerId);
            }

            if (!entry.type.isAssignableFrom(type)) {
                throw new NotificationConfigurationException(
                        "El proveedor " + providerId + " no es compatible con el tipo "
                                + type.getName());
            }

            defaultProviderByType.put(type, providerId);
            return this;
        }

        /**
         * Construye y devuelve un cliente de notificaciones listo para usar.
         *
         * <p>Valida la configuración completa antes de entregar el cliente:
         * <ul>
         *   <li>Los tres tipos obligatorios (Email, SMS, Push) deben tener un proveedor predeterminado.</li>
         *   <li>Cada default debe referenciar un proveedor registrado y compatible.</li>
         * </ul>
         *
         * <p>Cada llamada crea instancias nuevas de registro, validador y servicio, por lo
         * que el cliente devuelto está aislado de modificaciones posteriores del Builder.
         *
         * @return un {@link NotificationClient} listo para enviar notificaciones
         * @throws NotificationConfigurationException si la configuración es inválida
         */
        public NotificationClient build() {
            validateDefaults();

            DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
            for (Map.Entry<String, ProviderEntry> entry : providersById.entrySet()) {
                registry.register(entry.getKey(), entry.getValue().type, entry.getValue().sender);
            }
            for (Map.Entry<Class<? extends Notification>, String> entry : defaultProviderByType.entrySet()) {
                registry.setDefaultProvider(entry.getKey(), entry.getValue());
            }

            DefaultNotificationValidator validator = new DefaultNotificationValidator();
            validator.register(new EmailNotificationValidator());
            validator.register(new SmsNotificationValidator());
            validator.register(new PushNotificationValidator());

            DefaultNotificationService service = new DefaultNotificationService(validator, registry);
            if (retryPolicy != null) {
                return new RetryingNotificationClient(service, retryPolicy);
            }
            return service;
        }

        /**
         * Construye y devuelve un cliente asíncrono de notificaciones listo para usar.
         *
         * <p>Valida la configuración completa antes de entregar el cliente.
         * Requiere que se haya configurado un ejecutor mediante
         * {@link #withExecutor(java.util.concurrent.Executor)}.
         *
         * <p>Cada llamada crea instancias nuevas de registro, validador y servicio,
         * por lo que el cliente devuelto está aislado de modificaciones posteriores
         * del Builder.
         *
         * @return un {@link AsyncNotificationClient} listo para enviar notificaciones de forma asíncrona
         * @throws NotificationConfigurationException si la configuración es inválida
         * @throws IllegalStateException si no se ha configurado un ejecutor
         */
        public AsyncNotificationClient buildAsync() {
            validateDefaults();

            DefaultNotificationSenderRegistry registry = new DefaultNotificationSenderRegistry();
            for (Map.Entry<String, ProviderEntry> entry : providersById.entrySet()) {
                registry.register(entry.getKey(), entry.getValue().type, entry.getValue().sender);
            }
            for (Map.Entry<Class<? extends Notification>, String> entry : defaultProviderByType.entrySet()) {
                registry.setDefaultProvider(entry.getKey(), entry.getValue());
            }

            DefaultNotificationValidator validator = new DefaultNotificationValidator();
            validator.register(new EmailNotificationValidator());
            validator.register(new SmsNotificationValidator());
            validator.register(new PushNotificationValidator());

            DefaultNotificationService service = new DefaultNotificationService(validator, registry);
            NotificationClient client;
            if (retryPolicy != null) {
                client = new RetryingNotificationClient(service, retryPolicy);
            } else {
                client = service;
            }

            if (executor == null) {
                throw new IllegalStateException(
                        "No se ha configurado un ejecutor. Use withExecutor(Executor) antes de buildAsync().");
            }

            return new DefaultAsyncNotificationService(client, executor);
        }

        private void validateDefaults() {
            validateDefaultPresent(EmailNotification.class);
            validateDefaultPresent(SmsNotification.class);
            validateDefaultPresent(PushNotification.class);
        }

        private void validateDefaultPresent(Class<? extends Notification> type) {
            String providerId = defaultProviderByType.get(type);
            if (providerId == null) {
                throw new NotificationConfigurationException(
                        "No hay proveedor predeterminado configurado para el tipo: " + type.getName());
            }
            if (!providersById.containsKey(providerId)) {
                throw new NotificationConfigurationException(
                        "El proveedor predeterminado " + providerId + " no está registrado para el tipo: "
                                + type.getName());
            }
        }
    }
}
