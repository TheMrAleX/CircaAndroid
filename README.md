# Aproximado

Juego nativo Android de trivia numérica para **2–8 personas**: pasando un teléfono o con un teléfono por jugador en **LAN por Wi-Fi/hotspot**. 400 preguntas en español y 12 categorías, sin servidores externos, anuncios ni analytics. No requiere acceso a Internet.

**[Descargar APK firmado v1.0.7](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.7/Aproximado-v1.0.7-arm64-v8a.apk)** · [Release y SHA256](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.7)

**Firma y actualizaciones:** el workflow genera una firma temporal por ejecución; si difiere de la instalada, Android requiere desinstalar la anterior; se perderán sus datos locales. [Detalle de firma](RELEASING.md).

## LAN por Wi-Fi o hotspot

En **Jugar en red local**, cada persona pone su nombre. Una crea la sala y comparte el código y la IP:puerto; las demás se unen desde la misma Wi-Fi o conectadas al hotspot. La búsqueda de salas es opcional: también se puede escribir la dirección. El anfitrión configura y avanza; todos responden simultáneamente desde su teléfono. Con 2 o 3 jugadores y «Regla en vivo» activada por todos, se muestran sus movimientos en tiempo real. Con 4 o más, o si cualquiera lo apaga, las estimaciones permanecen privadas.

Reconexión con plaza reservada, confirmaciones idempotentes y tráfico local cifrado. Android exige el permiso `INTERNET` para sockets LAN; la app solo admite destinos IPv4 privados/link-local, sin servicios externos. El hotspot se activa desde Android. [Instrucciones, límites y solución de problemas](LAN.md).

Si termina el proceso del anfitrión, la sala termina y hay que crear otra. Algunas redes de invitados aíslan los teléfonos; en ese caso usa un hotspot propio.

## Cómo jugar pasando el teléfono

1. Elige jugadores, nombres, colores distintos, 5/10/15/20 rondas, categorías y dificultad.
2. Todos leen la pregunta y empieza el primer jugador.
3. Cada persona ajusta su estimación arrastrando el panel sobre la regla fija, con los botones finos o con el teclado numérico. Los conteos admiten solo enteros; las pocas medidas que lo necesitan indican su precisión decimal. Al confirmar aparece directamente el selector del siguiente jugador, con nombre, avatar, turno y ronda; el valor anterior desaparece.
4. Al terminar todos, aparecen la gráfica de barras y la respuesta real, con una leyenda de estimaciones y puntos. Más cerca da más puntos; las escalas logarítmicas comparan proporciones. Exacto: +2; a menos del 1 %: +1. Los empates comparten puesto y puntos.
5. Consulta el marcador y termina en el podio. Puedes salir y continuar una partida guardada. El selector bloquea capturas y la vista de recientes.

## Recorrido visual

- **Inicio:** fondo carbón, reglas laterales animadas, título azul con contorno y botones grandes.
- **Configuración:** tarjetas redondeadas, nombres editables, colores y filtros.
- **Selector:** regla fija y panel del color del jugador que sube o baja con el número en tiempo real; ajuste fino y teclado.
- **Revelación:** gráfica vertical de barras por estimación, línea de respuesta real, corona y leyenda de puntos.
- **Marcador y resultado:** puntuación acumulada, puestos compartidos en empate y podio final.

Novedades 1.0.7: **Regla en vivo** para salas de 2 o 3, con interruptor en Ajustes y consentimiento de todos. Indicadores mientras seleccionas y reglas ampliadas mientras esperas; la confirmación y la puntuación siguen independientes. Con 4–8 jugadores siempre es privado. Todos deben instalar 1.0.7 para jugar juntos. [Cambios completos](docs/releases/v1.0.7.md).

Se conserva la [espera animada de 1.0.6](docs/screens/waiting_orbit.png). [Selector en vivo](docs/screens/live_own_selector_320x640_1.3_8p.png) · [Reglas de rivales](docs/screens/live_watch_320x640_1.3_8p.png) · [Prueba interactiva animada](docs/screens/live_selector_interaction.png).

Ejemplos LAN: [crear sala](docs/screens/lan_entry_320x640_1.3_8p.png), [unirse](docs/screens/lan_join_form_320x640_1.3_8p.png) y [espera](docs/screens/lan_waiting_320x640_1.3_8p.png).

Ejemplos de partida: [selector de 8 jugadores](docs/screens/selector_320x640_1.3_8p.png), [revelación](docs/screens/reveal_320x640_1.3_8p.png) y [marcador](docs/screens/score_320x640_1.3_8p.png).

Capturas verificadas se guardan en [docs/screens](docs/screens). La especificación completa está en [docs/DESIGN_SPEC.md](docs/DESIGN_SPEC.md); ajustes y limitaciones se registran en [DECISIONS.md](DECISIONS.md).

## Compilar y comprobar

Requisitos: JDK 17, Python 3, Android SDK platform 35/build-tools 35.0.0 y licencias aceptadas. Define `ANDROID_HOME` o crea `local.properties` con `sdk.dir=/ruta/al/sdk`.

```sh
python3 scripts/validate_questions.py
python3 scripts/generate_database.py
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. Tests visuales con Paparazzi: `./gradlew :app:verifyPaparazziDebug`; para actualizar las referencias tras una revisión visual usa `./gradlew :app:recordPaparazziDebug`. informes en `app/build/reports/paparazzi`. La matriz cubre 360×800 (2/8 jugadores), 320×640 y 411×914 con fontScale 1.3. No requiere emulador. Los renders verifican layout; no garantizan 60 fps en hardware real.

Arquitectura: una Activity, Compose y Material 3 con componentes propios, MVVM/StateFlow, Room desde SQLite generado y motor puro Kotlin en `core`. Estado e historial privados en almacenamiento local; no se suben a servicios.

[DATA.md](DATA.md) explica cómo añadir preguntas y regenerar el asset. [RELEASING.md](RELEASING.md) documenta firma, secrets y publicación por tag. CI ejecuta validación, tests, lint y compilación en push/PR; release genera APK firmado y SHA256.

## Licencias

Código y datos originales bajo [MIT](LICENSE). Fuente Fredoka bajo [SIL OFL](docs/licenses/Fredoka-OFL.txt). Icono, confetti y clic sintetizado propios. **No está afiliada a ningún juego existente.**

## Entorno cloud

`bash scripts/cloud_setup.sh` prepara JDK 17 y SDK 35 en `/workspace/toolchains`, activa cachés escribibles, valida los datos y ejecuta tests/lint/build. No crea worktrees. Las instrucciones reutilizables se guardaron también en el borrador de configuración cloud; revisarlas y publicar el entorno es una acción separada de publicar una release en GitHub. Véase [docs/VALIDATION.md](docs/VALIDATION.md) para evidencias y límites.
