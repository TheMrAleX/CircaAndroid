# Aproximado — instrucciones de trabajo

Trabaja autónomamente: elige opciones razonables sin preguntar y registra decisiones en DECISIONS.md. Kotlin, Compose, Material 3 como base con componentes propios, MVVM/StateFlow, Room offline, una Activity, vertical, UI española. No usar marcas o assets de otros juegos. No añadir INTERNET, anuncios ni analytics. Nunca versionar secretos. Haz commits pequeños por fase y verifica datos, tests, lint y builds. Consulta docs/DESIGN_SPEC.md antes de cada pantalla. No declarar verificaciones que no se ejecutaron.

## Estado
- [x] 0 Entorno y proyecto
- [x] 1 Datos y SQLite
- [x] 2 Motor y tests
- [x] 3 Sistema visual y pantallas
- [x] 4 Calidad: 10 tests de motor, 4 de integración y 44 renders; lint sin incidencias; build debug
- [x] 5 Release: APK ARM64/R8 firmado publicado en v1.0.0; CI y release remotos correctos; SHA256 y firma del APK descargado verificados
- [x] 6 Documentación: README, DATA, RELEASING, DECISIONS y evidencias/capturas

Cada tarea cloud ya está aislada; utilizar este checkout, no crear worktrees salvo petición expresa.

## Revisión 1.0.1
- [x] Nueve pantallas revisadas; revelación sin etiquetas flotantes; selector con áreas separadas.
- [x] Motor: precisión independiente de la respuesta, extremos exactos y comparaciones lineales/logarítmicas.
- [x] 13 tests de motor, 4 de integración y 68 renders registrados; build debug y lint correctos.
- [x] Comparación final de las 68 referencias; 85 casos sin fallos ni omisiones, lint debug sin incidencias.
- [x] Release v1.0.1 ARM64 publicada; CI y release remotos correctos; APK descargado, firma y SHA256 verificados.

La petición de rediseño 1.0.1 sustituye las posiciones rígidas de la spec original; conservar jerarquía, legibilidad y privacidad. Justificaciones en DECISIONS.md.

## Revisión 1.0.2
- [x] Gráfica restaurada como vista principal; barras visibles para 2–8 jugadores.
- [x] Regla fija y panel de jugador móvil, con número en tiempo real.
- [x] Aviso de privacidad retirado; decimales y tamaño del número estables.
- [x] 99 casos: 15 tests de motor, 4 de integración y 80 capturas comparadas; lint debug sin incidencias y APK debug compilado.
- [ ] APK release, CI remoto y publicación de v1.0.2.
