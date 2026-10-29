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
    <version>1.0.0</version>
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

---

## Limitaciones actuales

- Los adaptadores son simulados: no realizan conexiones de red ni usan SDK de
  proveedores externos.
- `SENT` significa aceptación por el adaptador/proveedor; no implica entrega
  ni lectura por el destinatario.
- No hay envío asíncrono, reintentos automáticos, envío por lotes, plantillas
  ni eventos de estado.
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
