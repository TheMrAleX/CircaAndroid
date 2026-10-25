# Aproximado — instrucciones de trabajo

Trabaja autónomamente: elige opciones razonables sin preguntar y registra decisiones en DECISIONS.md. Kotlin, Compose, Material 3 como base con componentes propios, MVVM/StateFlow, Room offline, una Activity, vertical, UI española. No usar marcas o assets de otros juegos. Sin anuncios ni analytics. Excepción autorizada en 1.0.5: INTERNET es necesario para sockets LAN en Android; no usar servicios externos. Nunca versionar secretos. Haz commits pequeños por fase y verifica datos, tests, lint y builds. Consulta docs/DESIGN_SPEC.md antes de cada pantalla. No declarar verificaciones que no se ejecutaron.

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

## Revisión 1.0.4
- [x] Catálogo activo reemplazado: 400 preguntas cotidianas, cifras de referencia sin redondeo artificial.
- [x] Pase retirado; cabecera integrada y cambio directo de jugador con estado reiniciado.
- [x] Room 3 y migración conservadora para partidas antiguas.
- [x] 126 casos: 18 motor, 8 integración y 100 capturas; lint debug y build debug correctos. Revisión de números con cinco decimales, cifra completa y siguiente jugador en pantalla pequeña.
- [x] Release v1.0.4 publicada; CI y release remotos correctos; APK oficial descargado, SHA256/firma v2/ARM64 y 400 filas verificadas.

## Revisión 1.0.5
- [x] Motor LAN autoritativo y protocolo cifrado con pruebas de sockets reales.
- [x] Sala Wi-Fi/hotspot, descubrimiento local, entrada IP y reconexión.
- [x] UI e integración: 173 casos (28 motor/protocolo, 9 Android, 136 visuales), comparación visual correcta; lint debug sin incidencias y APK debug compilado. Build release/R8 firmado y lint release correctos.
- [x] v1.0.5 publicada; CI y release remotos correctos; APK oficial descargado y verificado (SHA256, firma v2, ARM64, servicio LAN y catálogo íntegro). Pruebas físicas Wi-Fi/hotspot pendientes por falta de dispositivos.

## Revisión 1.0.6
- [x] Espera LAN con anillo giratorio, EaseInOutCubic y contador accesible.
- [x] 174 casos correctos: 28 motor/protocolo, 9 Android, 136 capturas y 1 ciclo animado de 23 fotogramas. Comparación visual correcta; lint debug y build debug correctos.
- [x] Release 1.0.6 publicada; CI/release correctos, APK oficial descargado y verificado (SHA256, firma v2, versión 7, ARM64 y 400 preguntas).
El usuario informó que 1.0.5 funciona correctamente en su prueba; las pruebas físicas disponibles en este entorno siguen siendo ninguna.

## Revisión 1.0.7
- [x] Protocolo LAN 2 con borradores efímeros, contexto validado, coalescencia 8 Hz y consentimiento unánime; solo 2–3 plazas.
- [x] Interruptor persistente en Ajustes/sala, indicadores y reglas de lectura; confirmación independiente.
- [x] 214 casos correctos: 34 motor/protocolo, 10 Android, 168 capturas, 1 ciclo animado y 1 prueba interactiva del selector (71 fotogramas). Comparación visual correcta; lint debug/release sin incidencias, APK debug y release/R8 firmado compilados.
- [x] Release 1.0.7 publicada; CI/release correctos y APK oficial descargado y verificado (SHA256, firma v2, versión 8, ARM64 y 400 preguntas).

## Revisión 1.0.8
La petición actual sustituye MIT para las nuevas distribuciones: licencia propia de atribución y autoría; conservar los permisos MIT históricos y las licencias de terceros. Permitir redistribución con crédito y cambios identificados, prohibiendo suplantar al autor original.
- [x] Licencia, crédito al autor y archivo MIT histórico.
- [x] Protocolo LAN 3 y gráfico compartido de 2–8 barras; arrastre propio probado sobre una columna ajena, privacidad opcional y confirmaciones congeladas.
- [x] 235 casos correctos: 35 motor/protocolo, 10 Android, 188 capturas, 1 ciclo de espera y 1 interacción táctil (81 fotogramas). Comparación visual correcta; lint debug sin incidencias y APK debug compilado.
- [x] Build release/R8 firmado y lint release sin incidencias; licencias empaquetadas idénticas a los textos del repo.
- [x] Release 1.0.8 publicada; CI/release correctos. APK oficial descargado y verificado: SHA256, firma v2, versión/código 1.0.8/9, ARM64, 400 preguntas y licencias actuales/históricas.

## Revisión 1.0.9
- [x] 800 preguntas validadas, 400 anteriores intactas, 12 categorías; Room 4 con migración aditiva y fixture v3.
- [x] Historial común persistente local/LAN, registro al mostrar, invitados y abandono; sorteo de no vistas y reutilización antigua aleatoria.
- [x] 244 casos correctos, comparación de 188 capturas y dos pruebas visuales animadas/interactivas; lint debug/release sin incidencias, APK debug y release/R8 firmado compilados.
- [x] v1.0.9 publicada; CI/release correctos. APK oficial descargado: firma v2/SHA256, versión/código 1.0.9/10, ARM64, Room 4 con 800 filas y licencias verificados. Firma distinta de 1.0.8 documentada.

## Revisión 1.0.10
- [x] Configuración de splits para ARMv7, ARM64, x86 y x86_64, con APK universal.
- [x] Workflow y script local preparados para publicar/verificar ARMv7, ARM64 y universal.
- [x] 244 casos, comparación visual y lint debug/release correctos; splits y universal inspeccionados, APK locales firmados y DB/licencia/versiones verificadas.
- [ ] Publicación y verificación de los tres APK oficiales.
