# Plan Proyecto 2 - ÁvilaOS con sistema de archivos

Disco simulado, planificación de disco y suspensión de procesos.
**Entrega y defensa:** viernes de Semana 11, antes de las 7:00 AM.

> Marcar `[x]` al terminar cada tarea, con la rama o el PR donde quedó.
> Cada bloque tiene su rama `feat/...` y se integra a `develop` por **Pull Request comentado**.

## Estado de los bloques

| # | Bloque | Rama | Responsable | Estado |
|---|--------|------|-------------|--------|
| 0 | Organización y limpieza | `chore/limpieza-p1` | | Pendiente |
| 1 | Disco aislado | `feat/disk` | Gabriel | En progreso |
| 2 | Sistema de archivos | `feat/filesystem` | Kelvin | Completada |
| 3 | Procesos de E/S | `feat/io-processes` | Kelvin | En progreso |
| 4 | Suspensión (mediano plazo) | `feat/swap` | | Implementada en `develop` |
| 5 | Modo administrador / usuario | `feat/user-mode` | Kelvin | Completada |
| 6 | Interfaz gráfica | `feat/gui-p2` | | Pendiente |
| 7 | Persistencia JSON y configuración | `feat/json-p2` | | Pendiente |
| 8 | Métricas y gráfico del cabezal | `feat/disk-metrics` | | Pendiente |
| 9 | Informe, UML y defensa | `docs/informe-p2` | Todos | Pendiente |

**Orden sugerido por el enunciado:** 1 → 2 → 3 → 4. Los bloques 5 a 8 se pueden hacer en
paralelo cuando exista lo que necesitan (ver *Depende de* en cada bloque).

---

## Bloque 0 - Organización y limpieza

- [ ] Crear un Issue en GitHub por cada bloque y asignar responsable
- [ ] Dejar una sola instancia del computador (retirar `SimulationCluster` de la simulación)
- [ ] Retirar buffers productor-consumidor y procesos `PRODUCER`/`CONSUMER`
- [ ] Revisar el test que falla hoy: `MonitorPanelExportTest.eventLogShowsStartupProcessCreationAndTermination`
- [ ] Confirmar con las preparadoras dónde va el área de swap: el JSON de ejemplo llena los
      200 bloques y pide `swapSizeBlocks: 100` (ver Bloque 1)

## Bloque 1 - Disco aislado

**Depende de:** nada.

- [x] Enums: 7 estados, `IOOperationType`, `DiskPolicyType`, `UserMode`, `HeadDirection` (`feat/disk`)
- [x] `DiskBlockMap`: bloques numerados, asignación encadenada, liberar cadena, semáforo (`feat/disk`)
- [x] Interfaz `DiskSchedulingPolicy` + `DiskRequest` + `DiskSelection` (`feat/disk`)
- [x] Políticas FIFO, SSTF, SCAN, C-SCAN verificadas con el anexo y casos P1–P4 (`feat/disk`)
- [x] `DiskScheduler`: cola única de E/S protegida, cambio de política en caliente (`feat/disk`)
- [x] PR `feat/disk` → `develop` (PR #10, fusionado)
- [x] `Disk` (hardware) implementa `ClockListener`: avanza un ciclo por tick, el cabezal recorre
      N posiciones por ciclo (configurable) y cada bloque leído/escrito cuesta un ciclo
- [x] Al terminar una solicitud, el disco avisa para despertar al proceso
- [x] Área de swap de tamaño configurable, reservada al final del disco
- [ ] (Opcional) LOOK / C-LOOK

## Bloque 2 - Sistema de archivos

**Depende de:** Bloque 1 (`DiskBlockMap`).

- [x] Árbol propio de directorios y archivos (sin colecciones de Java)
- [x] Cada archivo: nombre, tamaño en bloques, dueño, permiso (público/privado), primer bloque
- [x] Crear archivo (falla si no hay bloques libres) y directorio
- [x] Renombrar archivo/directorio (update)
- [x] Eliminar archivo libera sus bloques; eliminar directorio borra todo su contenido
- [x] Resolver rutas tipo `/sistema/boot.bin`
- [x] Proteger el sistema de archivos con semáforo
- [x] Tests de CRUD sobre el disco

## Bloque 3 - Procesos de E/S

**Depende de:** Bloques 1 y 2.

- [x] PCB con operación (`IOOperationType`) y ruta del archivo
- [x] La interfaz solo crea el proceso; el CRUD lo ejecuta el proceso, nunca la ventana
- [x] Al obtener la CPU, el proceso genera la solicitud y pasa a Bloqueado (motivo E/S)
- [x] El disco atiende y el proceso vuelve a Listo
- [x] Cada operación sobre el disco cuenta como instrucción en modo sistema operativo
- [x] Las políticas de CPU del Proyecto 1 siguen funcionando

## Bloque 4 - Suspensión y planificador a mediano plazo

**Depende de:** Bloque 3. *El enunciado recomienda dejarlo para el final.*

- [x] Cola a largo plazo para procesos que no caben en RAM disponible
- [x] Elegir un proceso Bloqueado para suspender (primero elegible, incluida E/S activa)
- [x] Suspender y reactivar son solicitudes de E/S que pasan por la cola del disco
- [x] Ocupar/liberar bloques del swap (1 MB de RAM = 1 bloque)
- [x] Bloqueado/Suspendido pasa a Listo/Suspendido cuando ocurre su evento
- [x] Al liberarse memoria, reactivar un Listo/Suspendido
- [x] Log: "Se suspende al proceso P7 para admitir a P9"

## Bloque 5 - Modo administrador / usuario

**Depende de:** Bloque 2.

- [x] Selector de modo en la interfaz
- [x] Admin: todas las operaciones
- [x] Usuario: leer archivos propios o públicos; crear solo en sus directorios
- [x] Rechazar operaciones no permitidas explicando el motivo, sin detener la simulación

## Bloque 6 - Interfaz gráfica

**Depende de:** Bloques 1–3 (se puede maquetar antes con datos de prueba).

- [x] `JTree` del sistema de archivos; al seleccionar: ruta, tamaño, dueño, permisos, primer bloque
- [x] Mapa de bloques: libre/ocupado, qué archivo o proceso suspendido, posición y sentido del cabezal
- [ ] `JTable` de asignación: nombre, bloques, primer bloque, dueño, proceso creador, color
- [x] Cola de E/S del disco con la operación de cada solicitud
- [x] Selector de política de disco (cambio en caliente) y pausar/reanudar en Monitor
- [x] Colas Listo/Suspendido y Bloqueado/Suspendido, y motivo de cada bloqueo
- [ ] Log con decisiones del disco: "El disco atiende la solicitud de P4 en el bloque 87 (SSTF)"
- [ ] Validación de tipo y rango en todos los campos
- [ ] La interfaz observa al sistema: nada de lógica de ÁvilaOS en las ventanas

## Bloque 7 - Persistencia JSON y configuración

**Depende de:** Bloque 2 (sistema de archivos) y Bloque 3 (procesos).

- [x] Cargar el JSON del sistema de archivos (`diskConfig` + `fileSystem`)
- [x] Guardar el estado del sistema de archivos en JSON
- [x] Cargar el JSON de procesos (`globalConfig` + `computer` + `processes`)
- [x] Exigir ambos JSON al cargar un escenario del Proyecto 2
- [ ] Configuración nueva: nº de bloques, reserva real del swap, política inicial, posición inicial del cabezal

## Bloque 8 - Métricas

**Depende de:** Bloques 1 y 4.

- [x] Desplazamiento total del cabezal
- [x] Tiempo promedio de espera en la cola de E/S
- [x] Solicitudes atendidas por cada política
- [x] Cantidad de suspensiones y reactivaciones
- [x] Gráfico: desplazamiento acumulado del cabezal vs. tiempo
- [ ] Se mantienen las métricas del Proyecto 1

## Bloque 9 - Informe, UML y defensa

- [ ] Informe: clases y métodos principales, enums e interfaces
- [ ] 2 diagramas UML actualizados
- [ ] Conclusiones: comparación de políticas (desplazamiento y espera) y efecto de la suspensión
- [x] Probar que corre en NetBeans con Java 21+
- [ ] Commits equilibrados entre los 3 integrantes
- [ ] Issues cerrados y PRs comentados
- [ ] Todos conocen todos los módulos para la defensa

---

## Reglas del Proyecto 2

| Regla | Estándar |
|-------|----------|
| Estructuras de datos | Propias (prohibido `ArrayList`, `Queue`, `Stack`, `Vector` y colecciones de Java) |
| Hilos y semáforos | Los de Java (`Thread`, `java.util.concurrent.Semaphore`) para proteger estructuras compartidas |
| Enums obligatorios | Estados con suspensión, operación de E/S, política de disco, modo de usuario |
| Interfaces obligatorias | Política de disco (agregar una política no modifica el planificador) |
| Librerías externas | Solo gráficas (JFreeChart) y JSON (Jackson) |
| Java / IDE | Java 21+, debe correr en NetBeans |
| Commits | Descriptivos y cortos: `feat(disk): agrega política C-SCAN al planificador de disco` |
| Ramas | `feat/<bloque>` → PR comentado → `develop` → `master` |
| Issues | Uno por bloque, más uno por cada error encontrado |

## Flujo de trabajo

```
1. Actualizar:  git switch develop && git pull
2. Rama:        git switch -c feat/<bloque>
3. Commits:     pequeños y descriptivos
4. Push:        git push -u origin feat/<bloque>
5. PR:          feat/<bloque> -> develop, con comentario de un compañero
6. Marcar [x] en este plan y cerrar el issue
```

---

*Documento vivo: cada quien marca sus tareas al terminarlas.*
