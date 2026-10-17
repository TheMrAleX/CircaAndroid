# Aproximado — instrucciones de trabajo

Trabaja autónomamente: elige opciones razonables sin preguntar y registra decisiones en DECISIONS.md. Kotlin, Compose, Material 3 como base con componentes propios, MVVM/StateFlow, Room offline, una Activity, vertical, UI española. No usar marcas o assets de otros juegos. No añadir INTERNET, anuncios ni analytics. Nunca versionar secretos. Haz commits pequeños por fase y verifica datos, tests, lint y builds. Consulta docs/DESIGN_SPEC.md antes de cada pantalla. No declarar verificaciones que no se ejecutaron.

## Estado
- [x] 0 Entorno y proyecto
- [x] 1 Datos y SQLite
- [x] 2 Motor y tests
- [x] 3 Sistema visual y pantallas
- [x] 4 Calidad: 10 tests de motor, 4 de integración y 44 renders; lint sin incidencias; build debug
- [x] 5 Release: firma local ARM64/R8 verificada; workflow por tag configurado; estado remoto en RELEASING.md
- [x] 6 Documentación: README, DATA, RELEASING, DECISIONS y evidencias/capturas

Cada tarea cloud ya está aislada; utilizar este checkout, no crear worktrees salvo petición expresa.
