# Escenarios manuales del swap

En la aplicación, abre **Archivo → Cargar Proyecto 2 (dos JSON)...**. Selecciona
primero el archivo de disco y después el de procesos. En **Monitor**, pulsa
**Iniciar** o **Avanzar ciclo** y observa el registro de eventos.

| Prueba | JSON de disco | JSON de procesos | Resultado esperado |
|---|---|---|---|
| Presión de RAM | `config/project2-swap-disk.json` | `config/project2-swap-pressure.json` | Aparece «Se suspende...» y luego «Se reactiva...». Ambos procesos terminan. |
| Sin presión | `config/project2-swap-disk.json` | `config/project2-swap-no-pressure.json` | Ambos terminan sin eventos de swap. |
| Swap insuficiente | `config/project2-swap-disk.json` | `config/project2-swap-too-small.json` | El segundo espera en Nuevo hasta que haya RAM; no se suspende ninguno. |
| Archivo invade swap | `config/project2-swap-conflict-disk.json` | `config/project2-swap-pressure.json` | La carga se rechaza porque el archivo empieza en un bloque reservado. |

El escenario de presión usa 24 MB de RAM y dos procesos de 16 MB; reserva 20
bloques de swap. El archivo de lectura ocupa 20 bloques para mantener bloqueado
al primer proceso mientras el segundo espera memoria. Una unidad de memoria de
1 MB consume un bloque de swap.
