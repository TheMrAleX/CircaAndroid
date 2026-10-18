# Validación de Aproximado 1.0.2

## Alcance

- 430 preguntas en 12 categorías; JSON estricto e integridad SQLite.
- Motor Kotlin: ranking lineal/log, empates, bonus exacto y <1%, cero y fracciones, formato español, selección sin repetir y memoria reciente, partidas completas de 2 y 8 jugadores.
- Integración Robolectric con Room real: apertura del asset, partidas completas de cinco rondas con 2/8 jugadores, restauración del proceso detrás del pase privado, límites de configuración y estado corrupto.
- Paparazzi: 80 casos (nueve pantallas, formatos extremos, selector decimal/enorme, extremos mínimo/máximo del panel, 3,32 → 3,40, filtros, captura reportada y resultados empatados en cuatro configuraciones). 360×800 con 2/8 jugadores; 320×640 con 8 jugadores y fontScale 1.3; 411×914 con 2 jugadores y fontScale 1.3. Se revisaron selector, revelación, marcador, podio, intro, configuración y formatos enormes/decimales. Las listas se desplazan para acceder a todos los jugadores en pantallas pequeñas.
- Sin emulador ni dispositivo físico: no se afirma una medición de rendimiento, prueba de gestos táctiles en hardware ni inspección real de recientes. La protección se implementa con FLAG_SECURE exclusivamente en el selector y con eliminación inmediata de la pantalla al pasar turno.

Los renders fuerzan el estado final de las animaciones mediante LocalInspectionMode; comprueban composición y geometría, no el tiempo de cada secuencia. Las pruebas de integración ejecutan el motor y los checkpoints reales sin esa sustitución.

## Comandos reproducibles

```sh
python3 scripts/validate_questions.py
python3 scripts/generate_database.py
./gradlew :core:test :app:verifyPaparazziDebug :app:lintDebug :app:assembleDebug
python3 scripts/build_local_release.py
```

Para la máquina cloud, `bash scripts/cloud_setup.sh` activa JDK/SDK, respeta el proxy y el almacén de confianza del sistema, valida datos y ejecuta tests/lint/build. No modifica fuentes ni crea worktrees.

## Resultados locales de 1.0.2 (18 de octubre de 2026)

- 430 preguntas válidas; SQLite íntegra y cargada por Room.
- 15 tests JUnit del motor, 4 tests de integración y 80 renders Paparazzi: 99 casos, 0 fallos y 0 omitidos.
- verifyPaparazziDebug compara y pasa contra las 80 referencias registradas.
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
