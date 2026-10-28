# Arquitectura de la librería de notificaciones

## 1. Objetivos arquitectónicos

La arquitectura debe permitir: - Unificar la operación de envío. -
Mantener el núcleo independiente de frameworks y proveedores. -
Representar correctamente las diferencias entre canales. - Incorporar
nuevos proveedores sin cambiar el servicio de aplicación. - Probar la
lógica sin conexiones externas. - Mantener una API pública pequeña y
estable.

Se propone una arquitectura hexagonal ligera (puertos y adaptadores),
junto con Strategy, un registro de adaptadores y Builder.

## 2. Vista de alto nivel

``` text
Aplicación cliente
       |
       v
API pública: NotificationClient
       |
       v
Servicio de aplicación
  |        |          |
  |        |          +--> Validador
  |        +-------------> Registro de adaptadores
  |
  v
Puerto: NotificationSender
       |
       +--> Adaptador Email simulado
       +--> Adaptador SMS simulado
       +--> Adaptador Push simulado
```

El servicio y los modelos no deben depender de SDK de proveedores. Los
adaptadores dependen de las abstracciones del núcleo.

## 3. Estructura de paquetes propuesta

``` text
dev.notifications
├── api
│   └── NotificationClient.java
├── model
│   ├── Notification.java
│   ├── EmailNotification.java
│   ├── SmsNotification.java
│   ├── PushNotification.java
│   ├── NotificationResult.java
│   └── NotificationStatus.java
├── port
│   └── NotificationSender.java
├── application
│   ├── DefaultNotificationService.java
│   └── NotificationSenderRegistry.java
├── validation
│   └── NotificationValidator.java
├── exception
│   ├── NotificationValidationException.java
│   ├── NotificationConfigurationException.java
│   └── NotificationSendingException.java
├── config
│   └── NotificationConfiguration.java
└── adapter
    ├── email
    │   └── SimulatedEmailSender.java
    ├── sms
    │   └── SimulatedSmsSender.java
    └── push
        └── SimulatedPushSender.java
```

El paquete raíz es `dev.notifications`.

## 4. Modelos de dominio

### 4.1 Contrato común

``` java
public interface Notification {
    String recipient();
}
```

### 4.2 Email

``` java
public record EmailNotification(
    String recipient,
    String subject,
    String body
) implements Notification {}
```

### 4.3 SMS

``` java
public record SmsNotification(
    String recipient,
    String message
) implements Notification {}
```

### 4.4 Push

``` java
public record PushNotification(
    String recipient,
    String title,
    String body
) implements Notification {}
```

En este diseño, `recipient` significa dirección de email, número
telefónico o token/destino de dispositivo según el canal. La validación
y la documentación deben dejarlo claro. Si el proyecto crece, pueden
introducirse tipos de destinatario específicos.

Los records representan datos inmutables. Las validaciones de formato
deben centralizarse en el componente de validación, salvo invariantes
simples que se decida aplicar en los constructores.

## 5. API pública

``` java
public interface NotificationClient {
    NotificationResult send(Notification notification);
}
```

La aplicación cliente usa una sola operación pública. La asincronía
queda fuera de la primera versión.

La implementación por defecto será `DefaultNotificationService`. Los
detalles del registro y los adaptadores no forman parte de la API de uso
habitual.

## 6. Puerto de salida y Strategy

Contrato conceptual:

``` java
public interface NotificationSender<N extends Notification> {
    Class<N> notificationType();
    NotificationResult send(N notification);
}
```

Cada adaptador declara el tipo de notificación que acepta. Ejemplos: -
`SimulatedEmailSender` para `EmailNotification`. - `SimulatedSmsSender`
para `SmsNotification`. - `SimulatedPushSender` para `PushNotification`.

Los adaptadores simulados no deben hacer conexiones reales. Pueden
devolver resultados deterministas y simular fallos controlados en tests.

Strategy se aplica porque el envío concreto se delega a una
implementación intercambiable del puerto.

## 7. Registro y seguridad de tipos

El servicio recibe una notificación del tipo común `Notification`,
mientras que cada sender acepta un subtipo específico. Java no puede
inferir automáticamente que un `NotificationSender<?>` es compatible con
cualquier `Notification`.

Por tanto, el registro debe resolver esa diferencia de manera explícita
y segura.

Contrato interno sugerido:

``` java
public interface NotificationSenderRegistry {
    NotificationResult send(Notification notification);
}
```

Este contrato evita exponer genéricos no resueltos al servicio. Una
implementación interna puede mantener un mapa por tipo de notificación y
encapsular el despacho.

Reglas: - Cada adaptador debe declarar exactamente qué tipo acepta. - El
registro valida que la notificación sea compatible con el adaptador. -
No permitir casts genéricos no comprobados como mecanismo normal de
despacho. - Si se utiliza un cast inevitable por limitaciones del modelo
de tipos, debe estar encapsulado en un único punto y precedido por una
comprobación explícita; preferir una alternativa sin casts si mantiene
legibilidad. - Detectar tipos duplicados o proveedores ambiguos durante
la configuración. - Si no hay adaptador para una notificación, lanzar
`NotificationConfigurationException` con información segura.

Una alternativa igualmente válida es hacer que cada adaptador implemente
un contrato no genérico con `supports(Notification)` y un
`send(Notification)` que valide el tipo. Debe elegirse una estrategia,
documentarla y probarla; no mezclar ambas sin necesidad.

## 8. Selección de proveedor

El canal y el proveedor son conceptos distintos. Puede haber varios
proveedores de Email.

La configuración debe registrar adaptadores con identificadores únicos,
por ejemplo: - `email-primary` - `email-backup` - `sms-primary` -
`push-primary`

Para la primera versión: - El cliente usa una configuración explícita. -
Se elige un proveedor por canal/tipo para el envío normal. - Se permite
registrar más de un proveedor para un mismo canal. - No se implementa
failover automático. - No se reintenta automáticamente ante fallos.

La API concreta debe evitar ambigüedades entre el tipo de notificación y
el identificador de proveedor. El registro puede almacenar proveedores
por ID y mantener por separado la asociación entre tipo de notificación
y proveedor seleccionado.

## 9. Servicio de aplicación

Responsabilidades de `DefaultNotificationService`: 1. Recibir la
notificación. 2. Validarla. 3. Delegar en el registro/puerto
correspondiente. 4. Devolver el resultado del adaptador. 5. Propagar las
excepciones documentadas.

No debe: - Construir SDK de proveedores. - Contener un `switch` por
proveedor. - Conocer credenciales específicas de proveedores. -
Implementar validaciones de formato de cada canal. - Ocultar errores
devolviendo siempre éxito.

## 10. Validación

`NotificationValidator` delega en validadores registrados por tipo de
notificación. Cada validador declara qué tipo valida y no existe un
`switch` centralizado sobre Email, SMS, Push u otros canales.

-   Email: destinatario no vacío y formato razonable; asunto y cuerpo
    según las reglas acordadas.
-   SMS: número/destinatario no vacío y formato según la política
    elegida.
-   Push: destino no vacío y contenido requerido.

No se debe afirmar que una expresión regular garantiza la existencia de
un email, número o token. Las validaciones de formato son preliminares;
la aceptación real depende del proveedor.

Reglas de registro: - Cada validador debe declarar exactamente qué tipo
valida. - La configuración detecta registros duplicados o incompatibles.
-   La ausencia de un validador requerido genera un error claro, no una
    validación omitida silenciosamente. - La responsabilidad de
    validación está bien definida y no se duplica entre servicio,
    registro y adaptadores.

La estrategia de validación debe poder probarse sin red y cumple
Open/Closed: agregar un nuevo canal registra su validador sin modificar
`DefaultNotificationService`.

## 11. Resultados y excepciones

### Resultado

``` java
public enum NotificationStatus {
    SENT,
    FAILED
}
```

``` java
public record NotificationResult(
    NotificationStatus status,
    String providerMessageId,
    String errorCode,
    String errorMessage
) {}
```

El resultado debe garantizar invariantes coherentes: - `SENT`: no
contiene error y puede incluir un ID del proveedor. - `FAILED`: contiene
un código/mensaje de error y no se presenta como envío exitoso.

Los métodos de fábrica (`sent(...)`, `failed(...)`) pueden ayudar a
mantener esas invariantes.

`SENT` significa que el adaptador/proveedor aceptó la solicitud; no
implica entrega ni lectura.

### Excepciones

-   `NotificationValidationException`: entrada inválida.
-   `NotificationConfigurationException`: configuración inválida,
    proveedor ausente o registro ambiguo.
-   `NotificationSendingException`: error inesperado que impide
    completar el intento de envío.

Un fallo esperado del proveedor puede representarse mediante
`NotificationResult.failed(...)`; los errores inesperados se envuelven
en `NotificationSendingException`, conservando la causa. La política
exacta debe ser consistente en todos los adaptadores y quedar
documentada.

No incluir secretos ni contenido sensible en mensajes de error.

## 12. Configuración y Builder

API ilustrativa:

``` java
NotificationClient client = NotificationConfiguration.builder()
    .addSender("email-primary", EmailNotification.class, new SimulatedEmailSender())
    .addSender("sms-primary", SmsNotification.class, new SimulatedSmsSender())
    .addSender("push-primary", PushNotification.class, new SimulatedPushSender())
    .defaultProvider(EmailNotification.class, "email-primary")
    .defaultProvider(SmsNotification.class, "sms-primary")
    .defaultProvider(PushNotification.class, "push-primary")
    .build();
```

La firma exacta debe diseñarse para que el registro de proveedores
alternativos no genere ambigüedades. `build()` debe validar la
configuración y devolver un cliente listo para usar.

El Builder: - No debe leer archivos YAML o `.properties`. - Debe validar
duplicados, referencias a proveedores inexistentes y canales sin
proveedor por defecto. - Debe permitir configuración por código Java. -
No debe exigir credenciales en adaptadores simulados.

Para proveedores reales futuros, usar objetos de configuración
específicos y recibir secretos de forma segura desde la aplicación
anfitriona. No almacenar secretos reales en el repositorio.

## 13. Patrones y SOLID

-   Strategy: intercambiar implementaciones de envío.
-   Registry/resolución: encontrar el adaptador configurado.
-   Builder: construir una configuración coherente.
-   Factory: introducirla solamente si centraliza una creación real de
    adaptadores configurados.
-   Single Responsibility: separar servicio, validación, configuración y
    adaptadores.
-   Open/Closed: añadir implementaciones sin modificar el servicio.
-   Dependency Inversion: el núcleo depende de contratos.
-   Interface Segregation: contratos pequeños, orientados a una
    responsabilidad.

No añadir patrones solo para cumplir una lista. La simplicidad es un
criterio de calidad.

## 14. Dependencias permitidas

Para el núcleo inicial: - Java 21+. - Maven. - JUnit 5 para tests. -
Mockito para mocks, si resulta útil.

No agregar frameworks de aplicación ni SDK de proveedores. La necesidad
de cualquier otra dependencia debe justificarse.

## 15. Pruebas arquitectónicas y unitarias

Mínimo: - Un test de cada canal. - Validación antes del envío. - Tipo no
registrado. - Configuración duplicada/ambigua. - Fallo representado
mediante `FAILED`. - Excepción inesperada conservando la causa. -
Selección del proveedor configurado. - Prueba de extensibilidad con un
canal de prueba nuevo, sin modificar el servicio. - Comprobación de que
la ejecución simulada no requiere red.

## 16. Extensiones futuras

No forman parte del núcleo de la primera entrega: - `CompletableFuture`
y envío asíncrono. - Reintentos con backoff y clasificación de fallos. -
Idempotencia y prevención de duplicados. - Envío por lotes. -
Templates. - Eventos de estado. - Slack. - Integraciones reales con
proveedores. - Módulos Maven separados para SDK opcionales.

Antes de implementar una extensión, definir su contrato, semántica de
errores, pruebas y compatibilidad con la API pública.
