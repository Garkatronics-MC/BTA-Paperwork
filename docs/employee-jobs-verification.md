# Verificación manual — Oficios de empleados como strategies

**Verificado automáticamente:** `./gradlew test` → 78 tests, 0 fallos

> Plan de comprobación a mano de lo que no se puede automatizar sin el arranque de Minecraft.
> Lo automático (tests JUnit) vive en `src/test/java` y lo corre el CI: `./gradlew test`.
>
> Contexto: la refactorización convierte los oficios en *strategies* (`EmployeeJob`) y arregla
> tres cosas que estaban rotas: el género del empleado, el inventario y la HUD.
>
> Lo único que se ha comprobado en el entorno de desarrollo es lo automático. Las
> comprobaciones que necesitan el juego arrancado **quedan sin marcar**: no hay una instancia
> de BTA 8.0.1 donde ejecutarlas, así que no se dan por buenas.

## 0. Preparación

- Cliente BTA 8.0.1 con este mod + Brainless v1.5.0-pre3.
- Un empleado spawneado (`paperwork:employee`) y, para comparar, uno de la entidad antigua
  `paperwork:employee_farmer` (el stub deprecado).

## 1. Modelos, capas y género

> El empleado se dibuja en **capas**: el cuerpo (`main`), el uniforme, el peinado y las capas
> que aporta su oficio (el farmer: la bolsa). No hay una geometría por género: el género se
> marca con el peinado.

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 1.1 | plain | spawnea un empleado | cuerpo + uniforme + peinado, **sin** bolsa | ☐ |
| 1.2 | farmer | `FarmStick` sobre un área, clic en el empleado | se le añade la **capa de la bolsa** | ☐ |
| 1.3 | la bolsa va detrás | mira al farmer de frente y de espaldas | la bolsa se dibuja en la espalda, no delante | ☐ |
| 1.4 | el género no parpadea | mira al empleado de lado, gira la cámara | el **peinado no cambia** al mirarlo (antes cada lado tiraba su dado) | ☐ |
| 1.5 | el género sobrevive | recarga el mundo | el empleado conserva su peinado y su género | ☐ |
| 1.6 | los dos lados coinciden | compara el empleado en single player y en multijugador | se ve el mismo peinado en los dos | ☐ |

## 2. Trigo (el inventario sale del servidor)

> El trigo va en la **bolsa** (la capa del farmer), no en el cuerpo del empleado.

| # | Caso | Pasos | Resultado esperado | OK |
|---|---|---|---|---|
| 2.1 | sin trigo | empleado sin trigo | los huesos `wheat1/2/3` de la bolsa **no** se ven | ☐ |
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
| 5.2 | sus capas | ídem | salen con la **bolsa** (el oficio farmer), no como un plain | ☐ |

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
| 7.3 | builds | `./gradlew build` | pasa (es lo que corre el CI) | ☐ |
