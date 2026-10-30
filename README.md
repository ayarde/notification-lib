# notification-lib

Librería reutilizable de notificaciones en Java 21 o superior, construida con Maven,
independiente de frameworks y extensible. Proporciona una API común para enviar
notificaciones a través de distintos canales (Email, SMS y Push Notification)
sin depender de clases específicas de proveedores.

Los proveedores actuales son simulados: no realizan conexiones de red ni usan
SDK de proveedores externos. Devuelven resultados deterministas para permitir
pruebas deterministas sin credenciales ni servicios reales.

---

## Requisitos

- Java 21 o superior.
- Maven como herramienta de construcción.
- No se requiere Spring, Quarkus ni otro framework en el núcleo.

---

## Instalación y compilación local

Desde la raíz del repositorio:

```bash
./mvnw clean verify
```

Esto compila el proyecto, ejecuta las pruebas unitarias y las pruebas de
integración, y construye el artefacto JAR.

Para instalar el artefacto en el repositorio Maven local (`~/.m2/repository`):

```bash
./mvnw install
```

Una vez instalado, otro proyecto Maven puede consumir la librería como
dependencia. No hay publicación en un repositorio remoto en esta versión.

### Consumo desde otro proyecto Maven

Agregar la siguiente dependencia en el `pom.xml` del proyecto consumidor
(después de haber ejecutado `./mvnw install`):

```xml
<dependency>
    <groupId>dev.notifications</groupId>
    <artifactId>notification-lib</artifactId>
    <version>1.1.1</version>
</dependency>
```

El paquete raíz de la librería es `dev.notifications`.

---

## Quick Start

El siguiente ejemplo construye un `NotificationClient`, registra los tres
adaptadores simulados (Email, SMS y Push), configura el proveedor
predeterminado para cada tipo de notificación y envía una notificación de
cada canal:

```java
import dev.notifications.api.NotificationClient;
import dev.notifications.config.NotificationConfiguration;
import dev.notifications.adapter.email.SimulatedEmailSender;
import dev.notifications.adapter.sms.SimulatedSmsSender;
import dev.notifications.adapter.push.SimulatedPushSender;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.model.PushNotification;
import dev.notifications.model.NotificationResult;

NotificationClient client = NotificationConfiguration.builder()
    .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
    .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
    .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
    .defaultProvider(EmailNotification.class, "email-primary")
    .defaultProvider(SmsNotification.class, "sms-primary")
    .defaultProvider(PushNotification.class, "push-primary")
    .build();

// Email
NotificationResult emailResult = client.send(
    new EmailNotification("usuario@example.com", "Asunto", "Cuerpo del email"));
System.out.println("Email enviado: " + emailResult.isSent());

// SMS
NotificationResult smsResult = client.send(
    new SmsNotification("+5491112345678", "Mensaje de prueba de SMS"));
System.out.println("SMS enviado: " + smsResult.isSent());

// Push
NotificationResult pushResult = client.send(
    new PushNotification("token-dispositivo-123", "Título push", "Cuerpo de la notificación"));
System.out.println("Push enviado: " + pushResult.isSent());
```

`client.send(Notification)` es la única operación pública de la API. Acepta
cualquier tipo de notificación que implemente `Notification`.

---

## Ejemplos por canal

### Email

```java
EmailNotification email = new EmailNotification("usuario@example.com", "Asunto", "Cuerpo");
NotificationResult resultado = client.send(email);
```

El modelo `EmailNotification` requiere destinatario, asunto y cuerpo.

### SMS

```java
SmsNotification sms = new SmsNotification("+5491112345678", "Mensaje de prueba");
NotificationResult resultado = client.send(sms);
```

El modelo `SmsNotification` requiere destinatario y mensaje. No requiere
asunto.

### Push

```java
PushNotification push = new PushNotification("token-123", "Título", "Cuerpo");
NotificationResult resultado = client.send(push);
```

El modelo `PushNotification` requiere destino de dispositivo, título y cuerpo.

---

## Configuración y selección de proveedores

La configuración se realiza exclusivamente mediante código Java, sin archivos
YAML ni `.properties`.

### Registro de proveedores

Cada proveedor se registra con un identificador único (`providerId`), el tipo
de notificación que acepta y el adaptador correspondiente:

```java
.addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
.addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
.addSender("push-primary", PushNotification.class, new SimulatedPushSender())
```

Se pueden registrar varios proveedores para un mismo canal, siempre que cada
uno tenga un `providerId` único.

### Proveedor predeterminado por tipo

Se debe definir un proveedor predeterminado para cada tipo de notificación:

```java
.defaultProvider(EmailNotification.class, "email-primary")
.defaultProvider(SmsNotification.class, "sms-primary")
.defaultProvider(PushNotification.class, "push-primary")
```

Al enviar una notificación, el servicio resuelve el proveedor predeterminado
según el tipo de la notificación y lo invoca. No se implementa failover
automático: si el proveedor predeterminado falla, el error se propaga.

### Validación de la configuración

El método `build()` valida la configuración completa antes de entregar el
cliente y lanza `NotificationConfigurationException` si:

- Un `providerId` está duplicado.
- Un `providerId` referenciado en `defaultProvider` no existe.
- Un proveedor no es compatible con el tipo de notificación declarado.
- Falta un proveedor predeterminado para alguno de los tres tipos obligatorios
  (Email, SMS, Push).

Cada llamada a `build()` crea instancias nuevas de registro, validador y
servicio, por lo que los clientes construidos están aislados entre sí.

---

## Validación, errores y excepciones

### Validación antes del envío

El servicio valida la notificación antes de invocar al adaptador. Si los
campos obligatorios están vacíos o en blanco, o el formato es inválido, se
lanza `NotificationValidationException` y el adaptador no se invoca.

Ejemplo:

```java
try {
    client.send(new EmailNotification("", "Asunto", "Cuerpo"));
} catch (NotificationValidationException e) {
    // destinatario vacío: la notificación no llega al adaptador
}
```

### Tipos de excepción

| Excepción | Cuándo se lanza |
|-----------|-----------------|
| `NotificationValidationException` | Entrada inválida (campos vacíos, formato inválido). |
| `NotificationConfigurationException` | Configuración inválida, proveedor ausente, duplicados, referencias inexistentes. |
| `NotificationSendingException` | Error inesperado que impide completar el envío. Conserva la causa original. |

Todas heredan de `RuntimeException`.

### Resultado de envío

`NotificationResult.send()` devuelve un `NotificationResult` que distingue
éxito de fallo:

- `isSent()` devuelve `true` si el adaptador/proveedor aceptó el mensaje.
  Esto **no garantiza entrega ni lectura** por el destinatario.
- `isFailed()` devuelve `true` si el envío falló.
- `providerMessageId()` devuelve el identificador del mensaje en el proveedor
  (puede ser `null`).
- `errorCode()` y `errorMessage()` contienen información de error solo en
  estado `FAILED`.

Un fallo esperado del proveedor se representa mediante
`NotificationResult.failed(código, mensaje)`, no mediante una excepción. Los
errores inesperados se envuelven en `NotificationSendingException`.

### Reintentos configurables

El envío puede configurarse con una política de reintentos para manejar fallos
transitorios del proveedor. Cuando el resultado es `FAILED`, el cliente
reintenta el envío según la política hasta que obtenga un resultado `SENT` o
se agote la cantidad máxima de intentos.

No se reintenta ante `NotificationValidationException` ni
`NotificationConfigurationException`, ya que representan errores permanentes
que no se resuelven con un nuevo intento.

Ejemplo:

```java
import dev.notifications.retry.FixedDelayRetryPolicy;
import java.time.Duration;

NotificationClient client = NotificationConfiguration.builder()
    .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
    .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
    .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
    .defaultProvider(EmailNotification.class, "email-primary")
    .defaultProvider(SmsNotification.class, "sms-primary")
    .defaultProvider(PushNotification.class, "push-primary")
    .withRetryPolicy(new FixedDelayRetryPolicy(3, Duration.ofSeconds(2)))
    .build();
```

En el ejemplo anterior, si el proveedor devuelve `FAILED`, el cliente realiza
hasta 3 intentos en total (el primero más dos reintentos) con un retardo de
2 segundos entre intentos.

### Envío asíncrono

El envío asíncrono permite iniciar una operación sin bloquear al llamador.
El resultado se entrega mediante un `CompletableFuture<NotificationResult>`.
El consumidor proporciona un `Executor` donde se ejecutan las tareas de envío;
la librería no crea ni cierra ejecutores propios.

Ejemplo:

```java
import dev.notifications.api.AsyncNotificationClient;
import java.util.concurrent.Executors;

AsyncNotificationClient asyncClient = NotificationConfiguration.builder()
    .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
    .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
    .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
    .defaultProvider(EmailNotification.class, "email-primary")
    .defaultProvider(SmsNotification.class, "sms-primary")
    .defaultProvider(PushNotification.class, "push-primary")
    .withExecutor(Executors.newVirtualThreadPerTaskExecutor())
    .buildAsync();

CompletableFuture<NotificationResult> futuro = asyncClient.sendAsync(
    new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"));

// El llamador no está bloqueado; puede hacer otras cosas aquí.

NotificationResult resultado = futuro.join();
System.out.println("Resultado: " + resultado.isSent());
```

El API asíncrono es independiente del API síncrono (`NotificationClient`).
Ambos pueden usarse con la misma configuración.

Contrato de errores (coherente entre `sendAsync` y `sendBatchAsync`):
los errores de validación, configuración y envío no completan el futuro
excepcionalmente. Se devuelve un `NotificationResult` con estado `FAILED`
cuyo `errorCode` identifica el tipo de error (`ERROR_VALIDACION`,
`ERROR_CONFIGURACION` o `ERROR_ENVIO`) y cuyo `errorMessage` conserva el
mensaje de la causa original. Un resultado `FAILED` completa el futuro
normalmente, sin lanzar excepción. Solo los errores no capturados (por
ejemplo, instancias de `Error`) pueden producir una finalización excepcional
del futuro.

Si el `Executor` rechaza la tarea de envío de `sendAsync`, la excepción
`RejectedExecutionException` se propaga de forma síncrona antes de devolver
el futuro. En `sendBatchAsync`, la notificación correspondiente obtiene un
resultado `FAILED` con código `ERROR_EXECUTOR_RECHAZADO` y el lote continúa
con las notificaciones restantes.

Los reintentos configurables se componen con el envío asíncrono: si el cliente
síncrono envuelto incluye una política de reintentos, los reintentos se
ejecutan en el hilo del ejecutor, sin bloquear al llamador.

### Envío asíncrono por lotes

El envío por lotes permite enviar múltiples notificaciones en una sola operación
asíncrona. Cada notificación se envía de forma independiente usando el
`Executor` configurado por el consumidor, y los resultados se devuelven en una
lista que conserva el orden de las notificaciones de entrada.

El lote no es atómico: cada notificación produce su propio resultado
independiente. Un fallo en una notificación no impide procesar las demás.

Ejemplo:

```java
import dev.notifications.api.AsyncNotificationClient;
import dev.notifications.model.EmailNotification;
import dev.notifications.model.PushNotification;
import dev.notifications.model.SmsNotification;
import dev.notifications.model.NotificationResult;
import java.util.List;
import java.util.concurrent.Executors;

AsyncNotificationClient asyncClient = NotificationConfiguration.builder()
    .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
    .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
    .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
    .defaultProvider(EmailNotification.class, "email-primary")
    .defaultProvider(SmsNotification.class, "sms-primary")
    .defaultProvider(PushNotification.class, "push-primary")
    .withExecutor(Executors.newVirtualThreadPerTaskExecutor())
    .buildAsync();

List<dev.notifications.model.Notification> notificaciones = List.of(
    new EmailNotification("usuario@example.com", "Asunto", "Cuerpo"),
    new SmsNotification("+5491112345678", "Mensaje de prueba de SMS"),
    new PushNotification("token-dispositivo-123", "Título push", "Cuerpo"));

CompletableFuture<List<NotificationResult>> futuro =
    asyncClient.sendBatchAsync(notificaciones);

List<NotificationResult> resultados = futuro.join();

// resultados.get(0) corresponde al email
// resultados.get(1) corresponde al SMS
// resultados.get(2) corresponde al push

for (int i = 0; i < resultados.size(); i++) {
    System.out.println("Notificación " + i + " enviada: " + resultados.get(i).isSent());
}
```

El envío por lotes puede combinarse con reintentos configurables:

```java
import dev.notifications.retry.FixedDelayRetryPolicy;
import java.time.Duration;

AsyncNotificationClient asyncClient = NotificationConfiguration.builder()
    .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
    .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
    .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
    .defaultProvider(EmailNotification.class, "email-primary")
    .defaultProvider(SmsNotification.class, "sms-primary")
    .defaultProvider(PushNotification.class, "push-primary")
    .withRetryPolicy(new FixedDelayRetryPolicy(3, Duration.ofSeconds(2)))
    .withExecutor(Executors.newVirtualThreadPerTaskExecutor())
    .buildAsync();

CompletableFuture<List<NotificationResult>> futuro =
    asyncClient.sendBatchAsync(notificaciones);
```

Contrato de `sendBatchAsync`:

- Una lista vacía devuelve un futuro ya completado con una lista vacía.
- Una lista nula lanza `NullPointerException` de forma síncrona.
- Un elemento nulo en la lista lanza `NullPointerException` de forma síncrona
  antes de programar cualquier envío.
- Cada notificación se envía de forma independiente. Los resultados pueden ser
  `SENT` o `FAILED` en cada posición.
- Un resultado `FAILED` en una posición no impide que otras posiciones sean
  `SENT`.
- El futuro del lote se completa normalmente con la lista de resultados, incluso
  cuando hay fallos individuales.
- Si el `Executor` rechaza una tarea, esa notificación obtiene un resultado
  `FAILED` con código de error `ERROR_EXECUTOR_RECHAZADO` en su posición de la
  lista de resultados. El lote continúa con las notificaciones restantes y
  completa normalmente con una lista mixta de resultados `SENT` y `FAILED`
  cuando todos los errores son capturados. Los errores no capturados (por
  ejemplo, instancias de `Error`) pueden producir una finalización excepcional
  del futuro del lote.
- Algunas tareas previamente aceptadas por el `Executor` podrían seguir
  ejecutándose y no se garantiza su cancelación.
- Cancelar el futuro del lote no detiene necesariamente las tareas de envío que
  ya comenzaron en el adaptador.

---

## Proveedores soportados

La biblioteca incluye adaptadores simulados para los tres canales obligatorios.
No incluye integraciones reales con proveedores externos. La siguiente tabla
distingue qué está implementado, qué se investigó como referencia de diseño y
qué queda pendiente.

### Adaptadores simulados (implementados)

| Canal | Clase | Comportamiento |
|-------|-------|----------------|
| Email | `dev.notifications.adapter.email.SimulatedEmailSender` | Devuelve `SENT` determinista con ID simulado. Sin red. |
| SMS | `dev.notifications.adapter.sms.SimulatedSmsSender` | Devuelve `SENT` determinista con ID simulado. Sin red. |
| Push | `dev.notifications.adapter.push.SimulatedPushSender` | Devuelve `SENT` determinista con ID simulado. Sin red. |

### Proveedores investigados como referencia de diseño

Se consultó la documentación oficial de los siguientes proveedores para
verificar que los modelos y contratos de la biblioteca permiten construir
adaptadores realistas en el futuro. Ninguno está integrado en esta versión.

- **SendGrid (Email)**: Mail Send API v3
  `POST https://api.sendgrid.com/v3/mail/send`. Autenticación con API Key
  (`Authorization: Bearer`). Respuesta de aceptación: HTTP 202 (aceptación,
  no entrega). Identificador de mensaje disponible mediante encabezados o
  Event Webhook. URL: https://www.twilio.com/docs/sendgrid/api-reference/mail-send/mail-send

- **Twilio (SMS)**: Programmable Messaging API
  `POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json`.
  Autenticación Basic Auth. Respuesta de aceptación: JSON con `sid` y `status`
  (`queued`, `sending`, `sent`, `failed`, `delivered`, `undelivered`). Aceptación
  ≠ entrega. URL: https://www.twilio.com/docs/messaging/api/message-resource

- **Firebase Cloud Messaging (Push)**: HTTP v1 API
  `POST https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`.
  Autenticación OAuth 2.0 Bearer token. Respuesta de aceptación: JSON con
  `name` (ID del mensaje). Aceptación ≠ entrega. URL:
  https://firebase.google.com/docs/cloud-messaging/send/v1-api

### Integraciones no implementadas

SendGrid, Twilio y Firebase Cloud Messaging se listan únicamente como
referencia de diseño. No se implementaron adaptadores reales, no se agregaron
SDK de proveedores y no se realizan conexiones HTTP reales en esta versión.

---

## API Reference

### API pública síncrona

- `dev.notifications.api.NotificationClient` — Interfaz con una operación:
  `NotificationResult send(Notification notification)`. Envía cualquier tipo de
  notificación sin depender de clases específicas de proveedor.

### API pública asíncrona

- `dev.notifications.api.AsyncNotificationClient` — Interfaz con dos operaciones:
  - `CompletableFuture<NotificationResult> sendAsync(Notification notification)`.
    Ejecuta el envío en un `Executor` proporcionado por el consumidor.
  - `CompletableFuture<List<NotificationResult>> sendBatchAsync(List<Notification> notifications)`.
    Envía múltiples notificaciones de forma asíncrona y devuelve los resultados
    en el mismo orden de entrada.

### Modelos

- `dev.notifications.model.Notification` — Interfaz común con `String recipient()`.
- `dev.notifications.model.EmailNotification` — `recipient`, `subject`, `body`.
- `dev.notifications.model.SmsNotification` — `recipient`, `message`.
- `dev.notifications.model.PushNotification` — `recipient`, `title`, `body`.
- `dev.notifications.model.NotificationResult` — `status`, `providerMessageId`,
  `errorCode`, `errorMessage`. Fábricas: `sent(String)`, `sent()`, `failed(String, String)`.
- `dev.notifications.model.NotificationStatus` — `SENT`, `FAILED`.

### Configuración y Builder

- `dev.notifications.config.NotificationConfiguration.builder()` — Crea un Builder.
- `Builder.addSender(String, Class<? extends Notification>, NotificationSender<?>)` —
  Registra un proveedor con `providerId` único.
- `Builder.defaultProvider(Class<? extends Notification>, String)` — Define el
  proveedor predeterminado por tipo.
- `Builder.withRetryPolicy(RetryPolicy)` — Configura reintentos (opcional).
- `Builder.withExecutor(Executor)` — Configura el ejecutor para envío asíncrono.
- `Builder.build()` — Devuelve `NotificationClient` (síncrono).
- `Builder.buildAsync()` — Devuelve `AsyncNotificationClient`. Requiere executor.

### Puerto de envío

- `dev.notifications.port.NotificationSender<N extends Notification>` —
  `Class<N> notificationType()` y `NotificationResult send(N notification)`.
  Los adaptadores implementan esta interfaz para un canal concreto.

### Registro de adaptadores

- `dev.notifications.application.NotificationSenderRegistry` — Registro interno
  con métodos `register`, `setDefaultProvider`, `send`, `hasProvider`,
  `hasDefaultProvider`, `getDefaultProvider`.

### Validación

- `dev.notifications.validation.NotificationValidator` — `Class<? extends Notification> forType()`
  y `void validate(Notification)`. Delegado por `DefaultNotificationValidator`.

### Excepciones públicas

- `dev.notifications.exception.NotificationValidationException` — Entrada inválida.
- `dev.notifications.exception.NotificationConfigurationException` — Configuración inválida.
- `dev.notifications.exception.NotificationSendingException` — Error inesperado de envío.

### Políticas de reintento

- `dev.notifications.retry.RetryPolicy` — `maxAttempts()`, `shouldRetry(NotificationResult, int)`,
  `delayBeforeRetry(int)`.
- `dev.notifications.retry.FixedDelayRetryPolicy` — Retardo fijo entre intentos.

---

## Seguridad: mejores prácticas para manejar credenciales

La biblioteca no incluye un gestor de secretos, no carga variables de entorno
automáticamente, no cifra credenciales y no implementa mecanismos de autenticación
específicos de proveedores. Las siguientes recomendaciones aplican al código del
consumidor que utilice la biblioteca:

- No incluir API keys, tokens, contraseñas ni otros secretos reales en el código
  fuente, README, tests ni repositorio.
- No registrar credenciales ni datos sensibles en logs, excepciones ni mensajes de
  error.
- Mantener las credenciales fuera del control de versiones. Gestionarlas en el
  entorno de despliegue mediante gestores de secretos o variables de entorno del
  sistema operativo.
- Aplicar el principio de mínimo privilegio y rotar las credenciales según las
  políticas de cada proveedor.
- No incluir credenciales reales en ejemplos, capturas ni archivos de configuración
  versionados.

Estas son recomendaciones de uso, no capacidades implementadas por la biblioteca.

---

## Limitaciones actuales

- Los adaptadores son simulados: no realizan conexiones de red ni usan SDK de
  proveedores externos.
- `SENT` significa aceptación por el adaptador/proveedor; no implica entrega
  ni lectura por el destinatario.
- Un reintento puede generar un envío duplicado si el proveedor aceptó el
  mensaje pero la respuesta de aceptación se perdió antes de llegar al
  cliente. La librería no implementa idempotencia para prevenir duplicados.
- No hay envío por lotes atómico: cada notificación del lote se envía de forma independiente.
- No hay plantillas ni eventos de estado.
- Slack queda fuera de la primera versión.
- No se almacenan secretos, credenciales, tokens ni contenido sensible de
  notificaciones en el repositorio.
- El repositorio no contiene un archivo de licencia definido todavía.

---

## Cómo agregar un canal o proveedor nuevo

Para agregar un canal nuevo sin modificar `DefaultNotificationService`:

1. Crear un modelo que implemente `Notification`.
2. Crear un adaptador que implemente `NotificationSender<N>` donde `N` es el
   tipo de notificación del nuevo canal.
3. Crear un validador que implemente `NotificationValidator` para el nuevo
   tipo (opcional si no se requiere validación específica).
4. Registrar el adaptador en `NotificationConfiguration.builder()` mediante
   `addSender(providerId, tipo, adaptador)` y configurar su proveedor
   predeterminado con `defaultProvider(tipo, providerId)`.

El servicio de aplicación no contiene un `switch` por proveedor, por lo que
agregar un canal nuevo no requiere modificarlo.

---

## Comandos de desarrollo

- `./mvnw test`: ejecuta las pruebas unitarias.
- `./mvnw verify`: ejecuta pruebas unitarias y pruebas de integración, y
  construye el artefacto.
- `./mvnw clean verify`: limpia, compila, ejecuta todas las pruebas y
  construye el artefacto.
- `./mvnw install`: instala el artefacto en el repositorio Maven local.
---
