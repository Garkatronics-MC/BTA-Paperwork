# Verificación manual — Oficios de empleados como strategies

**Verificado automáticamente:** `./gradlew test` -> 81 tests, 0 failures

> Plan de comprobación a mano de lo que no se puede automatizar sin el arranque de Minecraft.
> Lo automático (tests JUnit) vive en `src/test/java` y lo corre el CI: `./gradlew test`.
>
> Contexto: la refactorización convierte los oficios en *strategies* (`EmployeeJob`) y arregla
> tres cosas que estaban rotas: el género del empleado, el inventario y la HUD.
>
> Lo único que se ha comprobado en el entorno de desarrollo es lo automático
> (`./gradlew test` -> 81 tests, 0 failures, y `./gradlew build` -> BUILD SUCCESSFUL).
> Las comprobaciones que necesitan el juego arrancado **quedan sin marcar**: no hay una
> instancia de BTA 8.0.1 donde ejecutarlas, así que no se dan por buenas.

## 0. Preparación

- Cliente BTA 8.0.1 con este mod + Brainless v1.5.0-pre3.
- Un empleado spawneado (`paperwork:employee`) y, para comparar, uno de la entidad antigua
  `paperwork:employee_farmer` (el stub deprecado).

## 1. Modelos y género

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 1.1 | plain × man | spawnea un empleado | modelo `geometry.paperwork.employee` (64×64) | ☐ |
| 1.2 | plain × woman | spawnea un empleado | modelo `geometry.paperwork.employee_woman` (con `hair`) | ☐ |
| 1.3 | farmer × man | `FarmStick` sobre un área, clic en el empleado | modelo `geometry.paperwork.employee_farmer` (96×64) | ☐ |
| 1.4 | farmer × woman | ídem | modelo `geometry.paperwork.employee_woman_farmer` | ☐ |
| 1.5 | el género no parpadea | mira al empleado de lado, gira la cámara | el modelo **no** cambia al mirarlo (antes sí: cada lado tiraba su dado) | ☐ |
| 1.6 | el género sobrevive | recarga el mundo | el empleado conserva el género | ☐ |

## 2. Trigo (el inventario sale del servidor)

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 2.1 | sin trigo | empleado sin trigo | los huesos `wheat1/2/3` **no** se ven | ☐ |
| 2.2 | 1 trigo | dale 1 trigo | se ve `wheat1` | ☐ |
| 2.3 | 32 trigo | dale 32 trigo | se ven `wheat1` y `wheat2` | ☐ |
| 2.4 | 64 trigo | dale 64 trigo | se ven `wheat1`, `wheat2` y `wheat3` | ☐ |
| 2.5 | el inventario se ve | el empleado lleva trigo | el cliente ve el trigo (antes el inventario del cliente estaba siempre vacío) | ☐ |
| 2.6 | el inventario sobrevive | recarga el mundo | el empleado conserva sus semillas/trigo (NBT) | ☐ |

## 3. HUD (se pintaba en ningún lado)

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 3.1 | HUD en single player | mira a un empleado | la HUD se pinta **sobre su cabeza**: alerta, estado, emoción y barra de trabajo | ☐ |
| 3.2 | HUD en multijugador | lo mismo, en un servidor | la HUD se pinta (otro camino de código) | ☐ |
| 3.3 | alerta de sin comida | empleado sin zona de comida | alerta `no_food_place` | ☐ |
| 3.4 | alerta de sin descanso | empleado con comida pero sin cama | alerta `no_rest_place` | ☐ |
| 3.5 | alerta propia del oficio | empleado **farmer** sin parcela | alerta `no_workplace` (antes ese icono no se usaba nunca) | ☐ |
| 3.6 | alerta buena | empleado con comida, cama y parcela | alerta `good` | ☐ |
| 3.7 | estados | deja al empleado con hambre/cansancio/peligro | iconos `hungry` / `tired` / `scared` según los umbrales | ☐ |
| 3.8 | barra de trabajo | un farmer trabajando | la barra se llena y se vacía con `stats.work` | ☐ |
| 3.9 | la HUD funciona con cualquier oficio | plain y farmer | la HUD se pinta igual en los dos (mismo camino de código) | ☐ |

## 4. Cambio de oficio en runtime

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 4.1 | plain → farmer | `FarmStick` sobre un área, clic en un empleado plain | se vuelve farmer **sin despawnear**: conserva inventario, stats y estaciones | ☐ |
| 4.2 | farmer → plain | cambia el oficio a plain | vuelve a ser plain; la parcela se suelta | ☐ |
| 4.3 | el cerebro se reconstruye | cambia de oficio con el empleado en marcha | no se queda con la task anterior colgada (el `Farm` se para) | ☐ |
| 4.4 | reasignar parcela | `FarmStick` sobre otra área, clic en el farmer | cambia la parcela, no reconstruye el cerebro de más | ☐ |
| 4.5 | el oficio sobrevive | recarga el mundo | el empleado sigue siendo farmer con su parcela (NBT) | ☐ |

## 5. Saves viejos

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 5.1 | entidad antigua | carga un mundo con empleados `employee_farmer` | spawnean como farmer (el stub fuerza el oficio en el constructor) | ☐ |
| 5.2 | su modelo | ídem | salen con el modelo del farmer, no del plain | ☐ |

## 6. Servidor dedicado

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 6.1 | arranque | arranca un servidor dedicado con el mod | arranca sin `NoClassDefFoundError` (antes `syncIcon()` llamaba a `TextureRegistry` desde el servidor) | ☐ |
| 6.2 | gameplay | el servidor calcula el trabajo del farmer | el farmer siembra/cosecha y su HUD se ve en el cliente | ☐ |

## 7. Extra: regresiones

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 7.1 | comer y dormir | asigna comida y cama con el `EmployeeStick` | el empleado va a comer y a dormir igual que antes | ☐ |
| 7.2 | el oficio no toca huesos ajenos | mira a un plain | el esqueleto del plain no tiene huesos de trigo: nada "parpadea" ni se ensucia el esqueleto compartido | ☐ |
| 7.3 | builds | `./gradlew build` | pasa (es lo que corre el CI) | ✔ (`./gradlew build` -> BUILD SUCCESSFUL) |
