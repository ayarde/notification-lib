# Instrucciones para agentes de coding

## Objetivo del repositorio

Construir una librería reutilizable de notificaciones en Java 21 o
superior, con Maven, independiente de frameworks y extensible. Debe
proporcionar una API común para Email, SMS y Push Notification, permitir
configurar proveedores mediante código Java y poder probarse sin
conexiones reales a servicios externos.

Antes de implementar, leer: 1. `docs/requirements.md` 2.
`docs/architecture.md` 3. `docs/implementation-plan.md`

Si existe una contradicción entre estos documentos, no elegir
silenciosamente una interpretación: informar el conflicto y proponer una
resolución.

## Reglas de trabajo

-   Inspeccionar el estado del repositorio y sus instrucciones antes de
    modificar archivos.
-   Trabajar en tareas pequeñas, delimitadas y verificables.
-   Implementar solamente la tarea solicitada. No adelantar etapas
    futuras sin autorización.
-   No cambiar la API pública ni las decisiones arquitectónicas
    aprobadas sin explicar el motivo y solicitar revisión.
-   No afirmar que un comando, test, build o revisión fue exitoso si no
    se ejecutó y comprobó su resultado.
-   Antes de terminar, revisar el diff completo y resumir archivos
    cambiados, verificaciones ejecutadas, resultados y limitaciones.
-   Si una decisión esencial no está especificada, señalarla y proponer
    alternativas antes de introducir una decisión difícil de revertir.

## Restricciones técnicas

-   Java 21 o superior y Maven.
-   El núcleo debe ser agnóstico a frameworks.
-   No introducir Spring, Quarkus u otro framework en el núcleo.
-   No agregar SDK de proveedores externos para la primera versión
    simulada.
-   No realizar conexiones HTTP reales: los proveedores iniciales deben
    simular el envío.
-   Configurar la librería mediante clases Java; no exigir YAML o
    `.properties`.
-   Evitar dependencias innecesarias y justificar cada dependencia
    nueva.
-   No utilizar `switch` centralizados por proveedor o canal dentro del
    servicio de aplicación.
-   No usar casts genéricos inseguros para despachar notificaciones.
    Encapsular y comprobar los límites de tipos en el registro si fueran
    necesarios.
-   No registrar API keys, tokens, códigos de verificación ni contenido
    sensible de notificaciones.

## Diseño y calidad

-   Mantener separados modelos, API pública, lógica de aplicación,
    validación, configuración y adaptadores.
-   Aplicar SOLID de forma práctica; no agregar patrones sin una
    necesidad concreta.
-   Los adaptadores son responsables de traducir el modelo común al
    contrato específico del proveedor.
-   Definir claramente qué significa que un proveedor haya aceptado un
    mensaje. No confundir aceptación con entrega o lectura.
-   Mantener consistentes los resultados y excepciones.
-   Los tests deben cubrir comportamiento positivo, errores, validación
    y extensibilidad.
-   Preferir tests deterministas que no requieran red ni credenciales.
-   Documentar cualquier limitación relevante.

## Verificaciones

Como mínimo, cuando el proyecto Maven ya exista:

``` bash
mvn test
```

En las etapas finales, ejecutar también:

``` bash
mvn clean verify
```

Si un comando falla, informar el fallo y no declarar la tarea terminada.
No cambiar tests para hacerlos pasar sin comprobar que la modificación
conserva los requisitos.

## Formato de cierre de cada tarea

Informar: 1. Resumen de implementación. 2. Archivos creados o
modificados. 3. Decisiones tomadas y justificación. 4. Comandos
ejecutados y resultados observados. 5. Criterios de aceptación
cubiertos. 6. Riesgos, limitaciones y siguiente paso recomendado.
