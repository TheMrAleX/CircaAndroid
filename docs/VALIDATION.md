# Validación de Aproximado 1.0.0

## Alcance

- 430 preguntas en 12 categorías; JSON estricto e integridad SQLite.
- Motor Kotlin: ranking lineal/log, empates, bonus exacto y <1%, cero y fracciones, formato español, selección sin repetir y memoria reciente, partidas completas de 2 y 8 jugadores.
- Integración Robolectric con Room real: apertura del asset, partidas completas de cinco rondas con 2/8 jugadores, restauración del proceso detrás del pase privado, límites de configuración y estado corrupto.
- Paparazzi: 44 casos (nueve pantallas y dos formatos extremos por cuatro configuraciones). 360×800 con 2/8 jugadores; 320×640 con 8 jugadores y fontScale 1.3; 411×914 con 2 jugadores y fontScale 1.3. Se revisaron selector, revelación, marcador, podio, intro, configuración y formatos enormes/decimales. Las listas se desplazan para acceder a todos los jugadores en pantallas pequeñas.
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

## Resultados locales (17 de octubre de 2026)

- 430 preguntas válidas; SQLite íntegra y cargada por Room.
- 10 tests JUnit del motor, 4 tests de integración y 44 renders Paparazzi: 58 casos, 0 fallos y 0 omitidos.
- verifyPaparazziDebug compara y pasa contra las 44 referencias registradas.
- lintDebug y lintRelease pasan; lintDebug informa “No issues found”.
- APK debug generado; release con minifyEnabled/shrinkResources completada y firma APK v2 comprobada con apksigner. Solo se empaqueta lib/arm64-v8a para la biblioteca nativa transitiva.
- Script cloud completo ejecutado con éxito; install_script/start_skill guardados como borrador. Publicación del entorno y restauración en una tarea nueva no verificadas.
- main enviada correctamente mediante Git. La API api.github.com devuelve Forbidden desde esta máquina; la página web pública de Actions sí es accesible. Publicación y estado remoto se registran en RELEASING.md después de enviar el tag.

