# ÁvilaOS

Simulador de un sistema operativo distribuido escrito en Java para la asignatura de Sistemas Operativos (trimestre 2627-1).

## Requisitos

- JDK 21 o superior
- Apache Maven 3.9+
- NetBeans es compatible; también puede abrirse como proyecto Maven desde cualquier IDE.

## Compilar, probar y ejecutar

```bash
mvn clean test
mvn exec:java
```

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
- [x] Monitor por computador y métricas globales de CPU y throughput; persistencia JSON validada.
- [x] Exportación del dashboard a PNG, CSV y HTML.

Desde **Archivo → Exportar Monitor** se puede guardar la vista actual como PNG,
CSV o HTML.

La pestaña **Computadores / Global** permite configurar el reloj, la latencia de
red y los computadores de la simulación. El monitor ejecuta cada ciclo en todos
los computadores y permite consultar sus colas, CPU y RAM de forma individual.
La carga de JSON reconstruye todos los computadores y conserva la asignación de
procesos y buffers.

Para estudiar la arquitectura, preparar una defensa y seguir pruebas manuales,
consulta la [Guía de defensa y pruebas](./GUIA_DEFENSA_Y_PRUEBAS.md).

## Convención de trabajo

- `master`: solo recibe cambios mediante Pull Requests desde `develop`.
- `develop`: rama de integración.
- `feat/...`, `fix/...`: ramas por funcionalidad, siempre con su Issue.
- Commits convencionales: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`.
