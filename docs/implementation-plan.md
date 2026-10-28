# Plan de implementación incremental

## Principios del plan

-   Cada etapa debe ser pequeña y revisable.
-   No empezar una etapa si sus dependencias no están aprobadas.
-   Mantener el proyecto compilable en cada punto razonable.
-   No implementar integraciones reales ni funcionalidades opcionales en
    la primera versión.
-   Ejecutar tests y revisar el diff antes de cerrar cada tarea.

## Etapa 0 --- Inspección y decisiones de diseño

### Tareas

-   Inspeccionar el repositorio y las instrucciones existentes.
-   Confirmar `groupId`, `artifactId`, paquete base y versión de Java.
-   Revisar `docs/requirements.md` y `docs/architecture.md`.
-   Cerrar el contrato de registro/dispatch y la semántica de errores.
-   Registrar cualquier decisión pendiente.

### Criterios de salida

-   No hay contradicciones conocidas entre requisitos y arquitectura.
-   La API pública propuesta está aprobada.
-   El agente presenta el plan antes de modificar código.

## Etapa 1 --- Fundación Maven

### Tareas

-   Crear `pom.xml`.
-   Configurar Java 21.
-   Configurar JUnit 5 y Mockito en alcance de test si se necesitan.
-   Crear estructura de paquetes.
-   Añadir un test mínimo.

### Verificación

``` bash
mvn test
```

### Criterios de salida

-   Maven compila el proyecto.
-   Los tests se ejecutan.
-   No se agregaron dependencias de frameworks ni SDK externos.

## Etapa 2 --- Modelos y contratos públicos

### Tareas

-   Crear `Notification`.
-   Crear `EmailNotification`, `SmsNotification` y `PushNotification`.
-   Crear `NotificationStatus` y `NotificationResult`.
-   Crear las excepciones documentadas.
-   Definir invariantes y validaciones básicas.

### Verificación

-   Tests de construcción de modelos.
-   Tests de invariantes de resultados.
-   Tests de validación de campos requeridos.

### Criterios de salida

-   Los modelos reflejan las diferencias entre canales.
-   No se exige un asunto para SMS.
-   Los resultados no pueden representar estados contradictorios.

## Etapa 3 --- Puerto, registro y servicio

### Tareas

-   Crear el contrato de envío.
-   Implementar el registro de adaptadores.
-   Implementar `NotificationValidator`.
-   Implementar `DefaultNotificationService`.
-   Implementar resolución de tipo y proveedor de forma segura.

### Verificación

-   El adaptador correcto recibe la notificación.
-   Los tipos no registrados generan un error de configuración claro.
-   No se invoca el adaptador si la validación falla.
-   No hay `switch` centralizados por proveedor en el servicio.
-   Los tests no dependen de casts genéricos inseguros.

### Criterios de salida

-   El servicio depende de abstracciones.
-   La lógica de selección queda encapsulada en el registro.

## Etapa 4 --- Adaptadores simulados

### Tareas

-   Implementar `SimulatedEmailSender`.
-   Implementar `SimulatedSmsSender`.
-   Implementar `SimulatedPushSender`.
-   Definir resultados simulados deterministas y escenarios de error
    testeables.

### Verificación

-   Tests de los tres canales.
-   Confirmar conservación de los campos de cada modelo.
-   Confirmar que no se realizan conexiones de red.

### Criterios de salida

-   Los tres canales pueden utilizarse desde la API común.
-   Los adaptadores cumplen el contrato de errores y resultados.

## Etapa 5 --- Configuración Java y selección de proveedores

### Tareas

-   Implementar `NotificationConfiguration` y Builder.
-   Permitir registrar adaptadores con IDs únicos.
-   Permitir elegir proveedor por defecto para cada tipo/canal.
-   Validar proveedores duplicados, referencias inexistentes y
    configuración incompleta.
-   Permitir proveedores alternativos sin failover automático.

### Verificación

-   Tests del Builder.
-   Tests de configuración inválida.
-   Tests que prueben la selección del proveedor elegido.
-   Tests de múltiples proveedores para el mismo canal.

### Criterios de salida

-   La librería se configura mediante código Java.
-   No requiere YAML ni `.properties`.
-   La selección del proveedor es explícita y determinista.

## Etapa 6 --- Tests de integración interna y extensibilidad

### Tareas

-   Crear tests de flujo completo desde `NotificationClient` hasta el
    adaptador simulado.
-   Añadir un canal de prueba, por ejemplo `TestNotification`, en el
    ámbito de test.
-   Verificar que el canal nuevo funciona sin modificar
    `DefaultNotificationService`.
-   Completar casos negativos y excepciones.

### Verificación

-   Ejecutar toda la suite.
-   Revisar que los tests fallen si se selecciona el adaptador
    incorrecto.
-   Revisar que los tests de extensibilidad no dependan de detalles
    internos innecesarios.

### Criterios de salida

-   La extensibilidad está demostrada por un test.
-   Las reglas de selección y error están cubiertas.

## Etapa 7 --- README y ejemplo de uso

### Tareas

-   Documentar requisitos de Java y Maven.
-   Documentar instalación/consumo de la dependencia.
-   Añadir Quick Start.
-   Añadir ejemplos para Email, SMS y Push.
-   Documentar configuración y selección de proveedores.
-   Documentar errores, limitaciones y cómo agregar un canal/proveedor.
-   Incluir los comandos para ejecutar tests y construir el artefacto.

### Verificación

-   Copiar y revisar los ejemplos contra la API real.
-   Ejecutar los comandos documentados cuando sea posible.
-   No documentar métodos que no existan.

### Criterios de salida

-   Un desarrollador puede entender y probar la librería desde el
    README.

## Etapa 8 --- Revisión final

### Tareas

-   Ejecutar `mvn clean verify`.
-   Revisar el diff completo.
-   Revisar dependencias y límites de arquitectura.
-   Revisar que no haya secretos ni datos sensibles en logs.
-   Revisar los criterios de aceptación de `requirements.md`.
-   Corregir defectos confirmados.

### Verificación final

``` bash
mvn clean verify
```

### Criterios de salida

-   Todos los tests pasan.
-   Todos los criterios obligatorios están cubiertos.
-   No quedan fallos conocidos sin documentar.
-   El informe final distingue hechos verificados de tareas pendientes.

## Plantilla de tarea para el agente

``` text
Implementa únicamente la etapa [N] del plan.

Antes de modificar archivos:
1. Lee AGENTS.md.
2. Lee docs/requirements.md y docs/architecture.md.
3. Inspecciona el estado actual del repositorio.
4. Enumera brevemente los archivos que planeas modificar.

Durante la tarea:
- Respeta la arquitectura aprobada.
- No implementes etapas futuras.
- Escribe o actualiza tests para el comportamiento introducido.
- No agregues dependencias innecesarias.
- Si una decisión fundamental no está especificada, detente y pregunta.

Al terminar:
1. Ejecuta las verificaciones correspondientes a esta etapa.
2. Ejecuta mvn test si el proyecto ya está configurado.
3. Revisa el diff.
4. Informa archivos cambiados, comandos ejecutados y resultados reales.
5. Relaciona los tests con los criterios de aceptación.
6. Lista riesgos o trabajo pendiente.

No declares completada la etapa si las verificaciones fallan.
```

## Definition of Done global

La primera versión se considera terminada cuando: - \[ \] Java 21+ y
Maven están configurados. - \[ \] Los tres canales obligatorios están
implementados. - \[ \] Existe una API pública común. - \[ \] La
configuración se realiza mediante Java. - \[ \] Se pueden registrar
múltiples proveedores para un canal y elegir el proveedor configurado. -
\[ \] Validación, configuración y errores de envío tienen semántica
diferenciada. - \[ \] Los tests no requieren conexiones reales. - \[ \]
Existe un test de extensibilidad con un canal adicional. - \[ \] El
README refleja la API real. - \[ \] `mvn clean verify` termina
correctamente. - \[ \] La revisión final no identifica incumplimientos
obligatorios conocidos.
