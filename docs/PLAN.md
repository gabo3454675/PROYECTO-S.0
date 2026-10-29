# Plan de Proyecto ÁvilaOS - Simulador de SO Distribuido

## Estado Actual

| Semana | Estado | Commit |
|--------|--------|--------|
| Semana 1 - Fundación | Completada | `77fac39` |
| Semana 2 - Kernel | Completada | `170ca8a` |
| Semana 3 - Sincronización y Políticas | Completada | `63ca653` |
| Semana 4 - GUI + Persistencia + Cierre | Pendiente | - |

---

## SEMANA 3: Sincronización (Semáforos + Buffers)

**Objetivo:** Semáforos propios, buffers acotados y productor-consumidor con latencia de red.

### Tareas

- [x] `Semaphore`: wait(PCB), signal(), cola de espera, mutex interno
- [x] `Buffer`: put(), get(), capacidad acotada, orden crítico (evitar deadlock)
- [x] `SyncManager`: gestión de buffers, latencia de red configurable
- [x] Acceso remoto: si process.computerId != buffer.hostComputerId, bloquear N ciclos (NETWORK_LATENCY)
- [x] Tests: 1 buffer cap=5, 2 productores, 2 consumidores, sin deadlocks

### Commits esperados
- `feat: semáforo propio con wait/signal y cola de espera`
- `feat: buffer acotado con productor-consumidor`
- `feat: acceso remoto con latencia de red`
- `test: pruebas de buffers y semáforos`

### Hecho cuando
- [x] Productor-consumidor funciona sin deadlocks
- [x] Acceso remoto bloquea y desbloquea correctamente
- [x] `mvn clean test` pasa (46 tests pasando sin errores)

---

## SEMANA 4: GUI + Cierre del Proyecto

**Objetivo:** Vista 1 (configuración), Vista 2 (monitor), persistencia JSON y entrega final.

### Tareas

- [x] Vista 1: configuración global y por computador; procesos y buffers por host
- [x] Vista 2: monitor por computador y panel global (CPU, colas, RAM, buffers, log)
- [x] Cambio de política en runtime desde la GUI
- [x] Persistencia: guardar/cargar configuración JSON multi-computador
- [x] Métricas visibles: utilización de CPU y throughput global y por computador
- [x] Exportación: PNG / CSV / HTML desde el dashboard
- [x] Tests completos (73 tests pasando): `mvn clean test` debe pasar sin errores

### Commits esperados
- `feat: vista 1 configuración con validaciones`
- `feat: vista 2 monitor en tiempo real`
- `feat: persistencia JSON guardar/cargar`
- `test: pruebas completas del proyecto`

### Hecho cuando
- [x] GUI funcional con ambas vistas
- [x] Configuración se guarda y carga en JSON
- [x] Todos los tests pasan en `mvn clean test`
- [x] Dashboard exportable a PNG, CSV y HTML
- [x] Configuración con varios computadores se carga y ejecuta de forma distribuida
- [ ] Repo GitHub limpio con issues cerrados

---

## Reglas del Proyecto

| Regla | Estándar |
|-------|----------|
| Estructuras de datos | Propias (sin `java.util.ArrayList/Queue/Stack/Vector`) |
| Semáforos | Implementación propia (no `java.util.concurrent`) |
| Java | 21+, compila en NetBeans |
| Commits | Convenionales: `feat:`, `fix:`, `test:`, `docs:` |
| Git | Rama `master` → PR → merge |
| Issues | Uno por semana, cerrar al completar |

---

## Flujo de Trabajo

```
1. Crear rama:  git checkout -b feat/semana-3-sync
2. Desarrollar: código de la semana
3. Commit:      git commit -m "feat: descripción"
4. Push:        git push origin feat/semana-3-sync
5. Merge:       Pull request a master
6. Cerrar issue correspondiente
```

---

*Documento vivo - actualizar al final de cada semana*
