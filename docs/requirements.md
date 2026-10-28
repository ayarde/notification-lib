# Requisitos de la librería de notificaciones

## 1. Objetivo

Crear una librería de notificaciones en Java que sea agnóstica a
frameworks, extensible y que unifique el envío a través de distintos
canales. El cambio de proveedor debe ser transparente para el código
cliente siempre que se mantenga el contrato público.

Este documento deriva sus requisitos funcionales y técnicos del
documento de especificación suministrado. Las decisiones de
implementación se desarrollan en `architecture.md`.

## 2. Requisitos obligatorios

### RQ-01 --- Plataforma

-   Java 21 o superior.
-   Maven como herramienta de construcción.
-   Código limpio, comprensible y fácil de mantener.

### RQ-02 --- Independencia de frameworks

El núcleo de la librería no debe requerir Spring, Quarkus ni otro
framework para funcionar.

### RQ-03 --- API común

Debe existir una API unificada para enviar notificaciones de distintos
canales. El código cliente debe invocar una operación común, sin
depender de clases específicas de proveedores.

### RQ-04 --- Canales obligatorios

La primera versión debe incluir: - Email. - SMS. - Push Notification.

Slack es opcional y queda fuera de la primera versión.

### RQ-05 --- Diferencias entre canales

La abstracción común no debe obligar a que todos los canales compartan
campos que no les corresponden. Por ejemplo: - Email necesita
destinatario, asunto y cuerpo. - SMS necesita destinatario telefónico y
mensaje. - Push necesita un destino de dispositivo y contenido de
notificación.

Los nombres y tipos definitivos se especifican en `architecture.md`.

### RQ-06 --- Configuración mediante Java

La configuración debe poder realizarse mediante clases Java, sin exigir
YAML o `.properties`.

Debe ser posible configurar credenciales y soportar más de un proveedor
para un mismo canal. La primera implementación puede usar adaptadores
simulados, sin credenciales reales.

### RQ-07 --- Manejo de errores

Debe distinguirse entre: - Errores de validación. - Errores de
configuración. - Fallos de envío. - Resultados de envío fallido que
puedan representarse sin excepción.

Los errores deben ser claros y fáciles de manejar mediante `try/catch`
cuando corresponda.

### RQ-08 --- Tests

Los tests unitarios deben usar mocks, stubs o simulaciones. No deben
requerir llamadas reales a proveedores.

### RQ-09 --- Documentación

El README del repositorio debe explicar: - Instalación y compilación. -
Inicio rápido. - Configuración de los canales y proveedores. - Ejemplos
de uso. - Cómo agregar un canal o proveedor.

## 3. Alcance de la primera versión

Incluye: - API síncrona. - Modelos específicos para Email, SMS y Push. -
Servicio de aplicación. - Contrato de adaptador de envío. -
Registro/resolución de adaptadores. - Validación. - Resultados y
excepciones documentados. - Builder de configuración Java. - Adaptadores
simulados para los tres canales. - Tests unitarios y un ejemplo de
uso. - README.

No incluye: - Integraciones reales con APIs externas. - Dependencias
obligatorias de frameworks. - Envío asíncrono. - Reintentos
automáticos. - Envío por lotes. - Templates. - Eventos de estado o
Pub/Sub. - Slack. - Failover automático entre proveedores.

Estas exclusiones son intencionales para mantener una primera versión
pequeña y verificable.

## 4. Requisitos opcionales para etapas futuras

-   Envío asíncrono mediante `CompletableFuture`.
-   Envío por lotes.
-   Política configurable de reintentos.
-   Validación específica de email, teléfono y destino Push.
-   Templates.
-   Eventos de estado de notificación.
-   Adaptadores reales para proveedores.
-   Dockerfile para ejecutar demos.

Cada capacidad futura debe incorporarse mediante una tarea independiente
y con tests propios.

## 5. Criterios de aceptación

### AC-01 --- Construcción

El proyecto compila con Java 21 o superior y Maven. `mvn clean verify`
termina correctamente.

### AC-02 --- Envío común

El cliente utiliza una misma operación pública para enviar los tres
tipos de notificación.

### AC-03 --- Selección de adaptador

Cada tipo de notificación se resuelve al adaptador configurado que
corresponda. Un adaptador de otro canal no debe recibirla.

### AC-04 --- Email

Una notificación Email conserva destinatario, asunto y cuerpo al llegar
al adaptador.

### AC-05 --- SMS

Una notificación SMS conserva destinatario y mensaje y no exige asunto.

### AC-06 --- Push

Una notificación Push conserva destino de dispositivo, título y cuerpo.

### AC-07 --- Validación

Una notificación inválida es rechazada antes de invocar el adaptador.

### AC-08 --- Errores

Los errores de validación, configuración y envío se distinguen de forma
documentada. Las excepciones inesperadas conservan su causa cuando
corresponda.

### AC-09 --- Resultado

El resultado diferencia éxito de fallo. El estado de éxito indica
aceptación por el adaptador/proveedor, no garantiza entrega ni lectura
por el destinatario.

### AC-10 --- Configuración

La librería puede construirse mediante código Java y registrar
adaptadores sin archivos de configuración obligatorios.

### AC-11 --- Múltiples proveedores

La configuración puede registrar proveedores identificados de manera
única para un mismo canal y definir cuál se selecciona. No se exige
failover automático.

### AC-12 --- Extensibilidad

Un canal nuevo de prueba puede agregarse mediante un modelo y adaptador
nuevos sin modificar la lógica central del servicio. El test debe
comprobar este comportamiento.

### AC-13 --- Sin red

La suite de tests y los ejemplos simulados no requieren credenciales ni
conexiones reales.

### AC-14 --- Documentación

El README documenta instalación, uso, configuración, errores y
extensión.

## 6. Consideraciones de seguridad

-   No incluir secretos reales en el repositorio.
-   No exponer credenciales ni tokens en excepciones o logs.
-   Evitar registrar cuerpos de notificación potencialmente sensibles.
-   No incluir datos personales innecesarios en identificadores de
    mensajes o métricas.

## 7. Fuera de alcance y decisiones pendientes

No se requiere una política de reintentos, garantía de entrega,
idempotencia distribuida, almacenamiento persistente ni métricas en esta
primera versión. Si se agregan en el futuro, deben especificarse por
separado.
