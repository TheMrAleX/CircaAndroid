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
- [x] Release v1.0.2 publicada; CI y workflow correctos; APK oficial descargado y verificado (SHA256, firma v2, ARM64, versión 3 y 430 preguntas).

## Revisión 1.0.3
- [x] `decimal_places` explícito en JSON/Room: 412 preguntas enteras, 18 medidas con 1–3 decimales.
- [x] Restricción común en regla, ajuste fino, teclado y confirmación; sin inferir precisión desde la respuesta durante el juego.
- [x] Gráfica lineal desde cero con alturas proporcionales y eje visible; dominio estable en logarítmicas.
- [x] Room 2 con migración 1→2 probada sobre DB real de 1.0.2, sin borrar preguntas ni preferencias.
- [x] 111 casos (17 motor, 6 integración y 88 visuales), lint debug y build debug correctos.
- [x] Comparación de las 88 referencias correcta.
- [x] Release v1.0.3 publicada; CI y release correctos; APK oficial descargado y verificado (firma v2, SHA256, ARM64 y DB v2 con 412 enteras/18 decimales).

## Revisión 1.0.4 en curso
- [x] Catálogo activo reemplazado: 400 preguntas cotidianas, cifras de referencia sin redondeo artificial.
- [x] Pase retirado; cabecera integrada y cambio directo de jugador con estado reiniciado.
- [x] Room 3 y migración conservadora para partidas antiguas.
- [x] 126 casos: 18 motor, 8 integración y 100 capturas; lint debug y build debug correctos. Revisión de números con cinco decimales, cifra completa y siguiente jugador en pantalla pequeña.
- [ ] Publicación y verificación de v1.0.4.
