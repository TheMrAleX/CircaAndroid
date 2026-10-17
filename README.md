# Aproximado

Juego nativo Android de trivia numérica para **2–8 personas** que se pasan un teléfono. 430 preguntas en español, 12 categorías y funcionamiento **100 % offline**: sin permiso de Internet, anuncios ni analytics.

**[Descargar APK firmado v1.0.0](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.0/Aproximado-v1.0.0-arm64-v8a.apk)** · [Release y SHA256](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.0)

## Cómo jugar

1. Elige jugadores, nombres, colores distintos, 5/10/15/20 rondas, categorías y dificultad.
2. Todos leen la pregunta. Pasa el teléfono a la persona indicada.
3. Cada persona ajusta su estimación arrastrando la regla vertical, con los botones finos o con el teclado numérico. Confirma sin enseñar la pantalla.
4. Al terminar todos, aparecen las barras de estimaciones y la respuesta real. Más cerca da más puntos; las escalas logarítmicas comparan proporciones. Exacto: +2; a menos del 1 %: +1. Los empates comparten puesto y puntos.
5. Consulta el marcador y termina en el podio. Puedes salir y continuar una partida guardada. El selector bloquea capturas y la vista de recientes.

## Recorrido visual

- **Inicio:** fondo carbón, reglas laterales animadas, título azul con contorno y botones grandes.
- **Configuración:** tarjetas redondeadas, nombres editables, colores y filtros.
- **Pase de turno:** pantalla del color del siguiente jugador sin estimaciones anteriores.
- **Selector:** regla deslizable, número gigante con contorno y sheet de color; entrada precisa con teclado.
- **Revelación:** barras individuales de color, respuesta de cristal, corona y confetti.
- **Marcador y resultado:** puntuación acumulada, puestos compartidos en empate y podio final.

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
