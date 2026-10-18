# Aproximado

Juego nativo Android de trivia numérica para **2–8 personas** que se pasan un teléfono. 400 preguntas en español, 12 categorías y funcionamiento **100 % offline**: sin permiso de Internet, anuncios ni analytics.

**[Descargar APK firmado v1.0.4](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.4/Aproximado-v1.0.4-arm64-v8a.apk)** · [Release y SHA256](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.4)

**Firma y actualizaciones:** si la release usa la firma temporal del workflow, requiere desinstalar APK de otra ejecución y se perderán la partida y el historial locales. Consulta el modo de firma en las notas de la release. [Detalle de firma y actualizaciones](RELEASING.md).

## Cómo jugar

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

Novedades 1.0.4: banco renovado de 400 preguntas reconocibles en 12 categorías, con las cifras de referencia a su precisión publicada: sin redondeos para facilitar aciertos. 351 valores enteros y 49 medidas con decimales. Cambio directo entre jugadores y cabecera integrada. Las partidas antiguas conservan sus preguntas al migrar la DB; las nuevas usan exclusivamente el catálogo nuevo. [Cambios completos](docs/releases/v1.0.4.md).

Ejemplos: [selector de 8 jugadores](docs/screens/selector_320x640_1.3_8p.png), [revelación](docs/screens/reveal_320x640_1.3_8p.png) y [marcador](docs/screens/score_320x640_1.3_8p.png).

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
