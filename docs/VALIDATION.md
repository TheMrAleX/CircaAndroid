# Validación de Aproximado 1.0.6

## Alcance

- 400 preguntas nuevas en 12 categorías (350 enteras y 50 con decimales publicados); JSON estricto e integridad SQLite.
- Motor Kotlin: ranking lineal/log, empates, bonus exacto y <1%, cero y fracciones, formato español, selección sin repetir y memoria reciente, partidas completas de 2 y 8 jugadores.
- Integración Robolectric con Room real: apertura del asset, partidas completas de cinco rondas con 2/8 jugadores, restauración directa en el selector del siguiente jugador, límites de configuración y estado corrupto.
- Paparazzi: 100 casos: las ocho pantallas vigentes, formatos extremos, extremos de la regla, decimales estables, filtros, empates, signos zodiacales enteros, cabecera del siguiente jugador, medidas con cinco decimales, respuesta completa y el caso 9,99/8,99 en cuatro configuraciones. 360×800 con 2/8 jugadores; 320×640 con 8 jugadores y fontScale 1.3; 411×914 con 2 jugadores y fontScale 1.3. Se revisaron selector, revelación, marcador, podio, intro, configuración y formatos enormes/decimales. Las listas se desplazan para acceder a todos los jugadores en pantallas pequeñas.
- Sin emulador ni dispositivo físico: no se afirma una medición de rendimiento, prueba de gestos táctiles en hardware ni inspección real de recientes. La protección se implementa con FLAG_SECURE exclusivamente en el selector y con eliminación inmediata de la pantalla al pasar turno.

La revisión 1.0.4 ejecutó 126 casos sin fallos, errores ni omisiones: 18 de motor, 8 de integración y 100 visuales. Se compararon las 100 referencias tras revisarlas; lint debug y build debug correctos. Las migraciones 1→2→3 y 2→3 se ejecutaron sobre bases reales de las versiones anteriores, con 400 preguntas activas y 430 archivadas, conservando las precisiones de las partidas antiguas. Se verificaron el bloqueo de confirmaciones tardías y la normalización de guardados PASS antiguos.

Los renders fuerzan el estado final de las animaciones mediante LocalInspectionMode; comprueban composición y geometría, no el tiempo de cada secuencia. Las pruebas de integración ejecutan el motor y los checkpoints reales sin esa sustitución.

## Comandos reproducibles

```sh
python3 scripts/validate_questions.py
python3 scripts/generate_database.py
./gradlew :core:test :app:verifyPaparazziDebug :app:lintDebug :app:assembleDebug
python3 scripts/build_local_release.py
```

Para la máquina cloud, `bash scripts/cloud_setup.sh` activa JDK/SDK, respeta el proxy y el almacén de confianza del sistema, valida datos y ejecuta tests/lint/build. No modifica fuentes ni crea worktrees.

## Resultados locales de 1.0.3 (18 de octubre de 2026)

- 430 preguntas válidas; SQLite íntegra y cargada por Room.
- 17 tests JUnit del motor, 6 tests de integración y 88 renders Paparazzi: 111 casos, 0 fallos y 0 omitidos.
- verifyPaparazziDebug compara y pasa contra las 88 referencias registradas.
- lintDebug y lintRelease pasan; lintDebug informa “No issues found”.
- APK debug generado; release con minifyEnabled/shrinkResources completada y firma APK v2 comprobada con apksigner. Solo se empaqueta lib/arm64-v8a para la biblioteca nativa transitiva.
- Script cloud completo ejecutado con éxito; install_script/start_skill guardados como borrador. Publicación del entorno y restauración en una tarea nueva no verificadas.
- main enviada correctamente mediante Git. La API api.github.com devuelve Forbidden desde esta máquina; la página web pública de Actions sí es accesible. Publicación y estado remoto se registran en RELEASING.md después de enviar el tag.


## Evidencia remota histórica: v1.0.0

CI #39070609124 y release #39070610379 completadas con éxito para `4b5012e`. Se verificó que [v1.0.0](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.0) adjunta APK y SHA256. La copia descargada pasó firma v2, checksum `7fb46502e3a3437cb392b3a73ab016d7bf7c470ab2c9289c72fa5d6c6de8e6d4`, recuento 430, integridad SQLite y ABI ARM64 exclusivo. Los primeros intentos fallaron antes de compilar en la acción externa de preparación del SDK; la corrección usa el SDK del runner Ubuntu 24.04 y las ejecuciones posteriores pasaron. No hay bloqueos restantes para la entrega del APK.

## Revisión visual 1.0.1

Se revisaron los renders de las nueve pantallas, especialmente 320×640 con fontScale 1.3. Se añadieron regresiones del ejemplo Argentina (23 frente a 28/38), ocho aciertos simultáneos con nombres largos, resultados logarítmicos enormes, selector con decimales y cifras enormes, y configuración desplazada hasta dificultad. Las listas muestran más jugadores al desplazarse: no intentan apilar ocho etiquetas en el mismo gráfico.

La revelación presenta los resultados de inmediato. Las animaciones de navegación, botones, marcador y podio permanecen; no se retiene una pantalla privada durante una transición. La cancelación de inercia está revisada en código; no se afirma una prueba gestual en un teléfono físico.

## Evidencia remota 1.0.1 (18 de octubre de 2026)

CI `39072467739` y release `39089680892` terminaron correctamente para `03267ea`. Se descargó el APK adjunto a v1.0.1 y se verificaron SHA256 `3b024458f846c3efe60c4e692c211a2060dacd551e0900bd1e178d6b1e404353`, firma v2, versión 1.0.1/código 2, ABI exclusivamente ARM64, SQLite íntegra con 430 preguntas y ausencia de INTERNET. Los informes locales finales contienen 13 tests de motor y 72 de app (4 integración + 68 visuales), sin fallos ni omisiones; lint debug y release contienen cero incidencias.

La firma publicada cambió respecto a v1.0.0: se requiere reinstalación, con pérdida de datos locales. Detalles y huellas en RELEASING.md. No se probó en dispositivo físico.

## Revisión visual y funcional 1.0.2

La gráfica vuelve a ser la vista principal. Se revisaron barras, línea de respuesta, leyenda, coronas, empates de ocho jugadores y números grandes en 360×800 (2/8 jugadores), 320×640 y 411×914 con fuente al 130%. Se revisaron el selector al mínimo, al máximo y las capturas 3,32/3,40: marcas fijas y panel/número con altura variable; tamaño de letra y decimales estables. Se retiró el aviso de privacidad del pase de turno.

Tests puros adicionales comprueban dirección, recorrido, límites y reversibilidad del arrastre, además de precisión decimal y ceros finales. Se mantienen las pruebas de partidas completas y restauración con Room. Sin prueba táctil en dispositivo físico ni medición de FPS.

## Evidencia remota 1.0.2 (18 de octubre de 2026)

CI `39090848418` y release `39090914423` terminaron correctamente para `0ff34b6`. Se descargaron el APK y el archivo SHA256 adjuntos a v1.0.2. Se verificaron checksum `a01186cdca1428fb36bc84aad163937593c5b3727ac4969d97259054fb29e732`, firma v2, versión 1.0.2/código 3, ABI exclusivamente ARM64, SQLite íntegra con 430 preguntas y ausencia de INTERNET. La comparación Paparazzi pasó las 80 referencias y los informes locales contienen 99 casos sin fallos ni omisiones (15 motor + 4 integración + 80 visuales). Lint debug y release sin incidencias.

Se compararon las firmas oficiales de 1.0.1 y 1.0.2: difieren, por lo que la instalación requiere desinstalar la anterior. El procedimiento y la pérdida de datos locales se documentan en RELEASING.md. Sin verificación en dispositivo físico.

## Revisión 1.0.3

Se añadieron comprobaciones del zodiaco (sin decimales), restricciones en todos los caminos de entrada, migración de una DB v1 real y comparación completa con el asset v2. Las 430 preguntas se validan con precisión explícita: 412 enteras y 18 medidas de 1–3 decimales.

La regresión 9,99/8,99 comprueba la relación de alturas 9,99/8,99 y una separación menor del 10% del gráfico. Se revisaron capturas de ese caso y del zodiaco, también con 8 jugadores y fuente al 130%. La gráfica lineal tiene origen cero; las marcas del eje respetan la precisión. La logarítmica usa los extremos de la pregunta, sin autozoom. No se modifican las reglas de puntuación ni se reescriben estimaciones históricas.

## Evidencia remota 1.0.3 (18 de octubre de 2026)

CI `39093624442` y release `39093675819` terminaron correctamente para `bd071e8`. Se descargaron el APK y SHA256 oficiales; checksum `7c83874ab90fb5d412665b91a28c31f1236c76087821aae1ba21a549e15fa435`. Se verificaron firma v2, versión 1.0.3/código 4, ABI ARM64 exclusivo, ausencia de INTERNET, integridad SQLite, esquema 2 y distribución de precisión 412/6/10/2 para 0/1/2/3 decimales. El APK incluye la migración y excluye la DB fixture de pruebas. Informes locales: 17 tests de motor y 94 de app (6 de integración + 88 visuales), sin fallos ni omitidos; comparación de las 88 referencias y lint debug/release correctos.

Las firmas oficiales de 1.0.2 y 1.0.3 difieren. La instalación pública requiere desinstalar la anterior y borra sus datos locales; la migración de Room probada solo puede preservar datos en una actualización firmada con clave compatible. Detalle en RELEASING.md. Sin prueba física ni medición de FPS.

## Publicación 1.0.4 verificada

CI `39144757551` y release `39144766164` correctos. APK oficial descargado: SHA256 `4878e463a6df6eb8aefc351aece68ce799f612b7572eeeba01a065b54098c027`, firma v2 RSA3072, versión 1.0.4/código 5, ARM64 exclusivo, sin INTERNET, DB3 íntegra y coincidencia completa de las 400 filas con los JSON. Lint debug/release local: sin incidencias; build release con R8 y firma correcto. El certificado difiere del de 1.0.3, con las consecuencias de instalación documentadas en RELEASING.md.

## LAN 1.0.5

- 173 casos locales correctos, sin omisiones: 18 de motor existente, 10 de motor/protocolo LAN, 8 de integración offline, 1 de integración Android LAN y 136 capturas comparadas.
- Sockets TCP reales: ocho participantes, cinco rondas, resultados idénticos, desconexión y reentrada con la misma plaza/respuesta. Proxy TCP de prueba corta una conexión y verifica reconexión automática y reenvío idempotente de una confirmación no reconocida.
- Privacidad de snapshots, ausencia de IDs/fuentes/respuestas antes de revelación, código incorrecto, sala llena, entradas tardías, estados inválidos, límites, confirmaciones duplicadas/de otra partida o ronda, valores no finitos/fuera de rango/precisión incorrecta y rechazo de manipulación/replay.
- Android simulado: foreground connectedDevice con notificación, recreación de ViewModel sin perder sala, resultados coherentes para anfitrión/invitado y preservación de preferencias offline. Compilación de API 35/minSdk 26.
- Pantallas LAN: crear/unirse, sala anfitrión/invitado, roster desplazado de ocho personas, filtros, espera, reconexión y selector propio. Matriz 360×800 (2/8), 320×640 y 411×914 con fontScale 1.3. Se revisaron renders pequeños y cabeceras/listas/botones fijos. Las filas parcialmente visibles por scroll no son texto truncado permanentemente.
- `:core:test :app:verifyPaparazziDebug :app:lintDebug :app:assembleDebug` correcto; lint debug sin incidencias. El catálogo sigue con 400 preguntas validadas y Room 3 sin cambios.

No hay `/dev/kvm`, emulador instalado ni teléfonos físicos disponibles. Las conexiones reales anteriores son loopback, no ensayos de radios Wi-Fi ni hotspot físico. Las comprobaciones de fabricante/red descritas en LAN.md quedan pendientes de dispositivos; no se declara una auditoría criptográfica externa ni garantía frente a usuarios maliciosos que posean el código.

Build release local 1.0.5 con R8, recursos reducidos y firma v2 correcto; lint release sin incidencias.

## Publicación 1.0.5 verificada

CI `39147800217` y release `39147825478` correctos. APK oficial descargado: SHA256 `c05771068822dfcaf6f3dd3e94815dab31caecfc811a3ce56fd5fdfe50ba63a6`, firma v2 RSA3072, versión 1.0.5/código 6 y ARM64 exclusivo. Catálogo SQLite 3 íntegro y coincidencia completa de sus 400 filas con los JSON. Manifest con servicio LAN connectedDevice no exportado, permisos normales LAN/foreground/wake lock y notificaciones opcionales; sin permisos de ubicación ni escaneo. Firma distinta de 1.0.4 y necesidad de desinstalar documentadas en README/RELEASING. Las pruebas físicas descritas en LAN.md siguen pendientes por falta de dispositivos.

## Espera animada 1.0.6

174 casos correctos: 28 de motor/protocolo, 9 Android, 136 capturas y 1 prueba de animación. Se revisó la espera en 320×640/fontScale 1.3 con ocho jugadores: título, contador y controles visibles; roster desplazable. Anillo propio alrededor del avatar, giro EaseInOutCubic de 2200 ms y pulsación de 1100 ms, actualizados solo en graphicsLayer.

Además de la fase fija de previews, WaitingMotionTest ejecuta el reloj real con LocalInspectionMode=false: 0–2200 ms a 10 fps, 23 fotogramas APNG y 22 imágenes distintas. Se revisaron fases de inicio/mitad y se comparó la referencia animada con verifyPaparazziDebug. Evidencia en docs/screens/waiting_orbit.png. No es una medición de rendimiento en hardware físico.

`:core:test :app:verifyPaparazziDebug :app:lintDebug :app:assembleDebug` correcto; lint debug sin incidencias. El usuario informó que 1.0.5 funciona correctamente en su prueba física. Este entorno no realizó nuevas pruebas en teléfonos para 1.0.6.

Build release local 1.0.6 con R8 y firma v2 correcto; lint release sin incidencias.

## Publicación 1.0.6 verificada

CI `39153767334` y release `39153772943` correctos. APK oficial descargado: SHA256 `4df4d328df6337c8efefe3a79db566d47a120b80913c3ba7ab3f91316d711b55`, firma v2 RSA3072, versión 1.0.6/código 7, ARM64 exclusivo y catálogo SQLite 3 íntegro con 400 filas idénticas a los JSON. Sin fixtures ni animación APNG de prueba empaquetadas. La firma cambió respecto a 1.0.5 y la consecuencia de instalación está documentada.

También se ejecutaron tres repeticiones limpias de los 28 tests de motor/protocolo, correctas. La validación animada usa renderizado automatizado; no mide fps físicos.

## Regla en vivo 1.0.7

214 casos correctos: 34 motor/protocolo (18 juego, 10 LAN y 6 de reglas en vivo), 10 Android (9 offline/preferencias, 1 LAN), 168 capturas estáticas y 2 pruebas animadas/interactivas. Comparación con verifyPaparazziDebug correcta; lint debug sin incidencias y APK debug compilado. Los 400 registros se validaron y la DB regenerada no presenta cambios.

Cobertura de política 2/3 frente a 4/8 plazas, consentimiento de anfitrión/invitados, desconexión/reconexión, retirada de borradores, precisión/rangos, épocas de consentimiento/ronda, mensajes atrasados y respuestas definitivas inmutables. Sockets reales de tres participantes: ráfaga de 1.000 cambios coalescida al último valor sin incrementar la revisión discreta, opt-out/reactivación y puntuación definitiva conservada. Robolectric verifica la recepción en Android, corte inmediato local, persistencia y limpieza de la vista al salir.

LiveSelectorTest mantiene el selector real conectado al reloj de Compose y ejecuta SetProgress, ajuste fino, actualización ajena, opt-out, reactivación y confirmación mediante semántica. Aserciones sobre valores publicados y confirmados; APNG de 71 fotogramas en docs/screens/live_selector_interaction.png. También se conserva el ciclo de espera de 23 fotogramas. La prueba offline genera valores con la misma precisión explícita del juego: se corrigió una suma de test que podía producir residuos binarios al sumar a objetivos decimales aleatorios, sin relajar la validación de la app.

Revisión visual en 320×640/fontScale 1.3: selección con dos oponentes, espera 2/3, privacidad a partir de 4, modo desactivado, nombres de 12 caracteres, jugador confirmado, números 1.400.000.000 y 0,5, Ajustes y sala. Las cifras largas en vivo se abrevian y su semántica conserva la cifra completa. Los nombres largos pueden ocupar dos líneas; contenido de espera/sheet desplazable y botón inferior fijo. No se atribuyen pruebas físicas Wi-Fi/hotspot ni medidas de fps a estos renders.

Build release local 1.0.7 con R8, reducción de recursos y firma v2 correcto; lint release sin incidencias.
