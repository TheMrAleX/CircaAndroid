# Decisiones

- Nombre original: Aproximado, paquete es.aproximado.app; licencia MIT.
- La imagen solo incluía JRE 21 (sin javac). Se instala Temurin JDK 17.0.14+7 verificando SHA256 oficial; AGP 8.9.1/Gradle 8.11.1, Android API 35 y minSdk 26.
- Rama local inicial sin commits: se crea main para la entrega solicitada.
- Fuentes de datos: hechos estables, unidades explícitas, valores convencionales redondeados y fechas en estadísticas históricas. Se excluyen récords humanos cambiantes sin fecha.
- Puntos empatados: competición por grupos de igual error; igual error obtiene igual puntuación. Orden visual estable por turno. Bonus +2 exacto, +1 si error numérico relativo menor que 1%; cero se trata explícitamente.
- Historial: últimas 80 preguntas; si los filtros agotan alternativas, se reutilizan las más antiguas, nunca se repite dentro de una partida.
- Spec visual guardada íntegra en docs/DESIGN_SPEC.md. No existían PNG de referencia. Arte vectorial, sonido sintetizado y fuente Fredoka OFL incluidos.
- En pantallas pequeñas y fontScale 1.3, se prioriza lectura: preguntas largas son desplazables, los chips tienen área táctil de 48dp y el sheet del selector aumenta de altura. El número se ajusta al ancho medido incluso si una escala de accesibilidad extrema exige bajar del mínimo nominal.
- Revelación: diferencias y puntos se presentan en una tira horizontal desplazable para que ocho jugadores con números largos sigan legibles. Se conservan barras, regla, respuesta de cristal y corona; el movimiento de puntos hacia arriba se sustituye por su lectura persistente. La secuencia usa interpolación ease-out, sin overshoot de barras para no representar valores fuera del rango.
- Pase privado y selector no usan fundido de salida: se elimina inmediatamente la pantalla anterior para no revelar una estimación durante la transición. Se omite el revelado circular en el pase por la misma prioridad de privacidad.
- La partida se persiste en cada acción de juego, con restauración segura al pase de turno si el proceso murió en el selector. La estimación aún no confirmada se descarta; las ya confirmadas se conservan. No se rota por diseño.
- Empates de ronda y final usan ranking de competición (1,1,3), no ranking denso; mismo error gana los mismos puntos. El orden de turno solo decide colocación visual. En el podio cada jugador empatado conserva su columna con altura idéntica y puesto compartido.
