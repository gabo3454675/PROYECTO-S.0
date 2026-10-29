# Guía de ÁvilaOS para entender, defender y probar el proyecto

Esta guía está pensada para estudiar el proyecto y hacer una demostración
ordenada. Combina explicaciones sencillas con los términos técnicos que pueden
aparecer durante la defensa.

## 1. ¿Qué es ÁvilaOS?

ÁvilaOS es un simulador educativo de un sistema operativo distribuido. Permite
configurar varios computadores virtuales, asignarles procesos y observar cómo
se ejecutan por ciclos. Los computadores comparten un reloj lógico y pueden
usar buffers compartidos; cuando un proceso accede a un buffer alojado en otro
computador, el simulador puede representar una latencia de red.

**Importante para la defensa:** los computadores son componentes simulados
dentro de una sola aplicación Java. No son máquinas físicas conectadas por una
red real. La latencia también es simulada: se cuenta en ciclos, no se mide con
paquetes reales.

## 2. El recorrido de una simulación

Una forma útil de entender el programa es seguir el camino desde la
configuración hasta el resultado:

1. Se carga un JSON o se configura la simulación desde la interfaz.
2. `SimulationCluster` administra los computadores y el estado global.
3. Cada computador tiene su propio `CPU`, `RAM` y `Kernel`.
4. El kernel admite los procesos que caben en memoria; los demás esperan en la
   cola de procesos nuevos.
5. El planificador elige un proceso listo para usar la CPU.
6. Cada ciclo ejecuta instrucciones, revisa plazos y atiende procesos
   bloqueados.
7. Los procesos productores y consumidores usan un buffer. Los semáforos
   coordinan el acceso y evitan que dos operaciones entren a la sección crítica
   al mismo tiempo.
8. El monitor presenta CPU, memoria, colas, buffers y métricas. Incluye una
   comparación de utilización acumulada de CPU por computador y un gráfico de
   procesos por estado para el computador seleccionado. La configuración
   también se puede guardar en JSON y el monitor se puede exportar.

Al pulsar **Avanzar 1 Ciclo**, el reloj de simulación aumenta una unidad y cada
computador ejecuta ese ciclo. Esto permite observar el comportamiento paso a
paso sin depender de una espera en tiempo real.

## 3. Conceptos técnicos explicados con palabras sencillas

| Término | Qué significa en este proyecto |
|---|---|
| **Sistema operativo (SO)** | El tema que se simula: administra CPU, memoria y procesos. |
| **Computador / nodo** | Una unidad simulada con su propia CPU, RAM, kernel y política de planificación. |
| **Proceso** | Un trabajo que compite por recursos. Se representa con un `PCB`. |
| **PCB (Process Control Block)** | El registro del proceso: nombre, computador asignado, estado, prioridad, plazo, tiempo restante y datos de sincronización. |
| **Ciclo** | Un paso discreto de la simulación. No equivale necesariamente a un segundo. |
| **Planificador** | La parte del kernel que decide cuál proceso listo se ejecuta. |
| **FCFS** | Ejecuta primero el proceso que llegó primero; normalmente no lo interrumpe por prioridad. |
| **Round Robin (RR)** | Reparte la CPU por turnos. El quantum limita cuántos ciclos puede usar un proceso antes de ceder el turno. |
| **EDF** | Da preferencia al proceso con el plazo (deadline) más próximo. |
| **Prioridad preventiva** | Puede interrumpir al proceso actual si hay otro listo con una prioridad que debe adelantarse. |
| **Cola NEW** | Procesos todavía pendientes de ser admitidos en memoria. |
| **Cola READY** | Procesos admitidos que esperan CPU. |
| **RUNNING** | Proceso que actualmente ocupa la CPU de un computador. |
| **BLOCKED** | Proceso que no puede avanzar hasta que ocurra algo: memoria disponible, recurso, dato o fin de latencia. |
| **TERMINATED** | Proceso que terminó su trabajo o cuyo plazo venció. |
| **Buffer acotado** | Espacio compartido de capacidad fija. Un productor no puede insertar si está lleno; un consumidor no puede extraer si está vacío. |
| **Semáforo** | Contador sincronizado que regula el acceso a un recurso y mantiene una cola de espera. |
| **Mutex** | Semáforo usado para permitir que solo una operación a la vez modifique el contenido del buffer. |
| **Latencia de red** | Número de ciclos de espera cuando el computador del proceso no es el host del buffer. |
| **Utilización de CPU** | Porcentaje de ciclos en que la CPU ejecutó al menos una instrucción: `ciclos ocupados / ciclos observados × 100`. |
| **Throughput** | Procesos completados por ciclo global: `procesos terminados / ciclos transcurridos`. Es una medida acumulada durante la simulación. |
| **Persistencia JSON** | Guardar y reconstruir la configuración desde un archivo de texto JSON. No es una base de datos. |

### Diferencia entre el ciclo y la duración configurada

La duración configurada del ciclo (en milisegundos) pertenece al reloj
automático `GlobalClock`. La interfaz gráfica actual no inicia ese reloj: su
control disponible es **Avanzar 1 Ciclo**, que avanza inmediatamente un ciclo
lógico en todos los computadores. Para una prueba reproducible, usa el avance
manual y anota el ciclo mostrado en pantalla. No presentes la GUI como una
ejecución automática hasta que se agregue un control para iniciar y pausar el
reloj.

## 4. Estructura del código

Las rutas siguientes son relativas a la carpeta raíz del repositorio:

| Carpeta o archivo | Responsabilidad |
|---|---|
| `avilaos/src/main/java/avilaos/Main.java` | Punto de entrada de la aplicación. |
| `avilaos/src/main/java/avilaos/simulation/SimulationCluster.java` | Coordina los computadores, ciclos globales y recursos compartidos. |
| `avilaos/src/main/java/avilaos/core/` | Componentes de hardware simulado, como reloj, computador, CPU y RAM. |
| `avilaos/src/main/java/avilaos/kernel/Kernel.java` | Ejecuta el ciclo del computador, gestiona colas, plazos, planificación y procesos. |
| `avilaos/src/main/java/avilaos/kernel/scheduler/` | Políticas de planificación disponibles. |
| `avilaos/src/main/java/avilaos/kernel/sync/` | Semáforos, buffers y administrador de sincronización. |
| `avilaos/src/main/java/avilaos/model/pcb/PCB.java` | Estado y datos de cada proceso. |
| `avilaos/src/main/java/avilaos/model/dto/` | Estructuras de datos para representar la configuración JSON. |
| `avilaos/src/main/java/avilaos/config/ConfigManager.java` | Guarda, valida y carga configuraciones; ofrece soporte para un kernel o un clúster. |
| `avilaos/src/main/java/avilaos/persistence/` | Serialización JSON y validación de la configuración. |
| `avilaos/src/main/java/avilaos/gui/` | Ventana, panel de configuración y monitor. |
| `avilaos/src/test/java/` | Pruebas automatizadas de las partes principales. |
| `config/default-simulation.json` | Ejemplo de dos computadores con procesos y buffers compartidos. |

## 5. Preparar el entorno en Windows

Se requiere JDK 21 o superior y Maven 3.9 o superior. Abre PowerShell en la
carpeta raíz, donde está `pom.xml`.

Comprueba las herramientas instaladas:

```powershell
java -version
mvn -version
```

La salida debe mostrar Java 21 o posterior y Maven 3.9 o posterior. Si Windows
indica que `mvn` no existe, instala Maven o abre una terminal donde Maven esté
agregado al `PATH`. Si el JDK no se encuentra, configura `JAVA_HOME` para que
apunte a la carpeta raíz del JDK y agrega `$env:JAVA_HOME\bin` al `PATH`.

## 6. Compilar, probar y ejecutar

Desde la raíz del proyecto:

```powershell
mvn clean test
mvn clean verify
mvn exec:java
```

- `mvn clean test` elimina resultados anteriores, compila y ejecuta las pruebas.
- `mvn clean verify` repite las pruebas y ejecuta las fases de verificación y
  empaquetado, incluido el JAR en `target`.
- `mvn exec:java` inicia la aplicación gráfica. Necesita una sesión de Windows
  con escritorio disponible.

Al momento de escribir esta guía, en el entorno de desarrollo se comprobó
`mvn clean verify` con **73 pruebas aprobadas, cero fallos y cero errores**.
Maven descargó correctamente el plugin de empaquetado que antes no podía
resolverse por DNS. Si vuelve a fallar una descarga, verifica tu conexión y
proxy/DNS y vuelve a ejecutar `mvn clean verify`; el error de descarga se
identifica porque Maven no puede acceder a `repo.maven.apache.org`.

El JAR de Maven se genera como artefacto de empaquetado; para ejecutar la
aplicación en desarrollo, el comando recomendado es `mvn exec:java`, que
resuelve también las dependencias del proyecto.

## 7. Prueba guiada con la configuración de ejemplo

Esta es una buena demostración para la defensa porque recorre la configuración,
la ejecución distribuida, la latencia y la sincronización.

### Preparación

1. Inicia la aplicación con `mvn exec:java`.
2. Abre **Archivo → Cargar Configuración (JSON)...**.
3. Selecciona `config/default-simulation.json` desde la raíz del repositorio.
4. Cambia a la pestaña **Computadores / Global** y verifica que aparecen los
   computadores `0` y `1`, con RAM de `1024 MB` y `2048 MB` respectivamente.
5. Comprueba que la duración global está en `100 ms` y la latencia de red en
   `2 ciclos`.
6. Revisa los procesos y buffers en la pestaña de configuración. El buffer
   `buf_alpha` está alojado en el computador `0`; `ConsumerA` pertenece al
   computador `1`, por lo que su acceso es remoto.

### Ejecución paso a paso

1. Ve a la pestaña **Monitor**.
2. Selecciona el computador `0` en el control **Computador**. Observa proceso en
   CPU, memoria y colas de ese nodo.
3. Pulsa **Avanzar 1 Ciclo** varias veces. El número de ciclo global debe
   aumentar y ambos computadores avanzan en cada paso.
4. Cambia la selección al computador `1`. Su CPU, RAM y colas son las de ese
   computador, no una copia del nodo `0`.
5. Revisa los buffers y el log. El consumidor remoto puede quedar bloqueado por
   latencia de red; después debe continuar cuando transcurran los ciclos de
   espera. Los productores y consumidores también pueden bloquearse si el
   buffer está lleno o vacío.
6. En **Computadores / Global**, compara ciclos ejecutados, utilización de CPU
   y throughput. En **Monitor**, consulta el gráfico de utilización acumulada
   de CPU para comparar los computadores y el gráfico circular para ver los
   estados del nodo seleccionado. Al principio el throughput puede ser `0`;
   aumenta cuando se completan procesos. Los gráficos muestran el estado y las
   métricas acumuladas actuales; no representan una serie histórica por ciclo.
7. Usa **Archivo → Exportar Monitor** y prueba CSV o HTML. Abre el archivo para
   comprobar que incluye el ciclo, colas y métricas.
8. Usa **Archivo → Guardar Configuración (JSON)...**. Carga ese archivo y
   verifica que se conservan ambos computadores, sus políticas, procesos y
   buffers.

Los valores exactos de las colas dependen del número de ciclos que avances. Para
comparar resultados, carga de nuevo el JSON antes de repetir la demostración y
usa la misma secuencia de pasos.

## 8. Otras pruebas manuales recomendadas

### A. Cambiar política

1. Selecciona un computador en el monitor.
2. Cambia la política en el selector.
3. Observa el valor actualizado en la configuración del computador.
4. Repite con Round Robin y cambia el quantum en la pestaña de configuración
   creando un computador con otro quantum.

El quantum es la cantidad máxima de ciclos consecutivos que se permiten en el
turno de Round Robin antes de devolver el proceso a READY, si aún no terminó.

### B. Crear computadores y procesos

1. En **Computadores / Global**, agrega un computador con un ID nuevo, RAM
   positiva, política y quantum positivo.
2. En **Procesos**, elige ese computador al crear un proceso de CPU.
3. Avanza varios ciclos y selecciónalo en el monitor.
4. Verifica que el proceso aparece en ese nodo y que su RAM no se contabiliza
   como memoria de otro computador.

### C. Productor y consumidor

1. Crea un buffer y elige su computador host.
2. Crea un productor que apunte al buffer y define intervalo y cantidad de
   elementos positivos.
3. Crea un consumidor para el mismo buffer. Puedes asignarlo a otro computador
   para probar el acceso remoto.
4. Avanza ciclos y observa el tamaño del buffer y las colas de bloqueo.
5. Con latencia de red en cero, el acceso remoto no añade espera de red. Con
   latencia positiva, debe aparecer la espera correspondiente.

### D. Rechazo de datos incompatibles

- Intenta crear un computador con un ID ya usado: debe aparecer un error.
- Intenta asignar un proceso a un computador que no existe: la configuración o
  la carga debe rechazarlo.
- Selecciona productor/consumidor sin buffer válido: la interfaz debe informar
  que falta el buffer o que los valores deben ser positivos.
- Carga un JSON inválido: debe mostrarse un mensaje de validación, no una carga
  parcial presentada como exitosa.

## 9. Qué comprueban las pruebas automatizadas

Ejecuta la suite completa con:

```powershell
mvn clean test
```

Entre otras cosas, las pruebas comprueban:

- las políticas del planificador y las estructuras de datos;
- semáforos, buffers y productor-consumidor;
- serialización y validación del JSON;
- la asignación de procesos al computador indicado;
- la ejecución de todos los computadores en un ciclo global;
- el desbloqueo en el kernel dueño del proceso;
- el guardado y la carga de configuraciones multi-computador;
- la exportación del monitor a PNG, CSV y HTML, incluyendo escaping de valores.

Una prueba automatizada comprueba un comportamiento controlado en código. No
sustituye la inspección visual de la interfaz ni las pruebas manuales descritas
arriba.

## 10. Cómo presentar el proyecto en la defensa

### Resumen de un minuto

> ÁvilaOS es un simulador educativo de un sistema operativo distribuido
> implementado en Java. Modela varios computadores, cada uno con CPU, RAM,
> kernel y política de planificación. Un clúster avanza todos los nodos con un
> ciclo lógico común. Los procesos pasan por colas de estados, y productores y
> consumidores coordinan el acceso a buffers mediante semáforos. La aplicación
> permite configurar y persistir la simulación en JSON, consultar métricas y
> exportar el monitor.

### Orden sugerido para exponer

1. Explica el objetivo del simulador y aclara que los nodos y la red son
   simulados.
2. Muestra el JSON y señala computadores, procesos, políticas y buffers.
3. Explica `SimulationCluster` como coordinador y `Kernel` como administrador
   de cada computador.
4. Describe los estados NEW, READY, RUNNING, BLOCKED y TERMINATED.
5. Muestra el buffer y cuenta cómo los semáforos controlan espacio, elementos y
   exclusión mutua.
6. Avanza ciclos en ambos computadores y muestra las métricas.
7. Guarda y vuelve a cargar la configuración; termina con una exportación.

### Preguntas frecuentes

**¿Es un sistema operativo real?**

No. Es una simulación educativa de algunos conceptos de administración de
procesos, memoria, CPU y sincronización.

**¿Los computadores se comunican por una red física?**

No. Se ejecutan en el mismo programa; la ubicación del buffer y una cantidad de
ciclos configurable representan el acceso remoto.

**¿Qué diferencia hay entre READY y BLOCKED?**

READY significa que el proceso puede ejecutarse, pero espera turno de CPU.
BLOCKED significa que no puede continuar hasta que se cumpla una condición.

**¿Para qué se usan los semáforos del buffer?**

Los semáforos de espacios vacíos y elementos disponibles controlan la capacidad
y disponibilidad. El mutex protege la operación crítica sobre el contenido.

**¿Qué representa el quantum?**

El máximo de ciclos concedidos en un turno de Round Robin antes de que el
proceso ceda CPU, mientras siga ejecutándose.

**¿Qué significa throughput?**

Cuántos procesos terminan por ciclo global observado. Es rendimiento de la
simulación, no instrucciones por segundo de un computador real.

**¿Qué se guarda en el JSON?**

Duración y latencia globales, computadores y sus políticas, procesos y buffers.
La configuración se puede reconstruir para ejecutar una nueva simulación.

**¿Cómo se comprueba que los cambios no rompieron el proyecto?**

Con `mvn clean test` para la suite y `mvn clean verify` para añadir la
verificación y el empaquetado. Después se inicia la interfaz con
`mvn exec:java` y se recorren los escenarios manuales.

## 11. Si algo no funciona

| Síntoma | Qué revisar |
|---|---|
| `mvn` no se reconoce | Maven no está instalado o no está configurado en `PATH`. |
| Maven no puede descargar un plugin o dependencia | Conexión, DNS, proxy y acceso a `repo.maven.apache.org`; luego repite `mvn clean verify`. |
| Error de versión de Java | Comprueba `java -version`, `mvn -version` y `JAVA_HOME`; el proyecto compila con release 21. |
| La GUI no aparece | Ejecuta desde una sesión de Windows con escritorio; no desde una consola sin entorno gráfico. Revisa el error completo de la terminal. |
| El JSON no carga | Asegúrate de seleccionar `config/default-simulation.json` desde la raíz o valida que el archivo no se haya editado con valores incompatibles. |
| El throughput permanece en cero | Avanza más ciclos y comprueba que los procesos tengan tiempo suficiente y no hayan vencido su deadline. |
| Un buffer permanece vacío o lleno | Revisa que productores y consumidores apunten al mismo ID de buffer y que el buffer exista. |

La prueba de empaquetado fue resuelta en el entorno de desarrollo:
`mvn clean verify` concluyó con BUILD SUCCESS y generó el JAR. El lanzador de depuración de
VS Code, en cambio, informó un fallo Maven sin exponer aquí un diagnóstico
concreto, aun cuando la verificación por consola pasó. Por eso se recomienda
`mvn exec:java` para ejecutar la aplicación y consultar la salida completa de
Maven si el lanzador del IDE presenta otro error.
