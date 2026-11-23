# ÁvilaOS

Simulador de un sistema operativo distribuido escrito en Java para la asignatura de Sistemas Operativos (trimestre 2627-1).

## Requisitos

- JDK 21 o superior
- Apache Maven 3.9+
- NetBeans es compatible; también puede abrirse como proyecto Maven desde cualquier IDE.

## Compilar, probar y ejecutar

```bash
mvn clean test
mvn clean verify
mvn exec:java
```

`mvn clean verify` ejecuta las pruebas y empaqueta el proyecto.

`mvn exec:java` inicia la aplicación gráfica y requiere un entorno de escritorio.

## Estructura

```
avilaos/src/main/java/avilaos/
├── config/      configuración de la simulación
├── core/        reloj global, CPU y RAM
├── gui/         configuración y monitor gráfico
├── kernel/      procesos, planificación, memoria y sincronización
├── metrics/     snapshots de métricas
├── model/       enums, interfaces, PCB y DTOs
├── persistence/ serialización y validación JSON
└── util/        estructuras de datos propias
avilaos/src/test/java/ pruebas unitarias
config/                  configuración de ejemplo
docs/                    plan y arquitectura
```

## Estado del proyecto

- [x] Fundación, hardware, kernel, planificación y sincronización.
- [x] Configuración global y por computador; simulación distribuida con buffers compartidos.
- [x] Monitor con comparación gráfica de CPU por computador y distribución de procesos por estado; métricas por nodo y globales de throughput temporal, respuesta, equidad, sincronización y productividad.
- [x] Exportación del dashboard a PNG, CSV y HTML.

Desde **Archivo → Exportar Monitor** se puede guardar la vista actual como PNG,
CSV o HTML.

La pestaña **Computadores / Global** permite configurar el reloj, la latencia de
red y los computadores de la simulación. El monitor ejecuta cada ciclo en todos
los computadores y permite consultar sus colas, CPU y RAM de forma individual.
**Avanzar ciclo** permite el avance paso a paso; **Iniciar** y **Pausar** activan
o detienen el avance automático según la duración configurada. La carga de JSON
reconstruye la configuración de los computadores, procesos y buffers; no guarda
un punto de control de una simulación ya iniciada.
Al iniciar se carga `config/default-simulation.json` para mostrar computadores,
procesos y buffers de ejemplo. Desde **Archivo → Cargar Configuración (JSON)...**
se puede reemplazar por otro escenario.
En **Configuración**, selecciona una o varias filas de procesos o buffers y
pulsa **Eliminar seleccionados** para quitarlos de la simulación. No se puede
eliminar un buffer que todavía usa algún proceso. **Reiniciar escenario**, en
el monitor, vuelve a cargar el último JSON abierto y descarta ciclos, buffers,
métricas y cambios hechos manualmente desde la interfaz.

Para estudiar la arquitectura, preparar una defensa y seguir pruebas manuales,
consulta la [Guía de defensa y pruebas](./GUIA_DEFENSA_Y_PRUEBAS.md).
La versión breve para exponer está en [Guía rápida de defensa (PDF)](./docs/GUIA_RAPIDA_DEFENSA.pdf),
con versiones editables en [Word](./docs/GUIA_RAPIDA_DEFENSA.docx) y
[Markdown](./docs/GUIA_RAPIDA_DEFENSA.md).
La estructura real de componentes se describe en [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md).
El [informe del Proyecto 1 (PDF)](./docs/INFORME_PROYECTO_1.pdf) resume el diseño,
los diagramas UML, las pruebas y el cumplimiento del enunciado. La versión
editable es [INFORME_PROYECTO_1.docx](./docs/INFORME_PROYECTO_1.docx).

## Convención de trabajo

- `master`: solo recibe cambios mediante Pull Requests desde `develop`.
- `develop`: rama de integración.
- `feat/...`, `fix/...`: ramas por funcionalidad, siempre con su Issue.
- Commits convencionales: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`.
