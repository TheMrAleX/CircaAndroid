# Decisiones

- Nombre original: Aproximado, paquete es.aproximado.app; licencia MIT.
- JDK 21 ya instalado: AGP 8.9.1/Gradle 8.11.1 soportan esta JVM; se reutiliza en lugar de instalar JDK 17. Android API 35 y minSdk 26.
- Rama local inicial sin commits: se crea main para la entrega solicitada.
- Fuentes de datos: hechos estables, unidades explícitas, valores convencionales redondeados y fechas en estadísticas históricas. Se excluyen récords humanos cambiantes sin fecha.
- Puntos empatados: competición densa por grupos de igual error; igual error obtiene igual puntuación. Orden visual estable por turno. Bonus +2 exacto, +1 si error numérico relativo menor que 1%; cero se trata explícitamente.
- Historial: últimas 80 preguntas; si los filtros agotan alternativas, se reutilizan las más antiguas, nunca se repite dentro de una partida.
