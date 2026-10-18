# Preguntas offline

El catálogo activo de 1.0.4 contiene **400 preguntas en 12 categorías**: 350 conteos/valores enteros y 50 medidas con decimales. Cada categoría tiene al menos 25. Los JSON son la fuente de verdad; `questions.db` es el asset SQLite que abre Room mediante `createFromAsset`.

Añade objetos en `data/questions/<categoria>.json`. Campos: `id` único y estable, `category`, `text_es`, `answer` numérico, `unit` (texto o null), `min_plausible`, `max_plausible`, `scale` (`linear`/`log`), `difficulty` (1–3), `as_of_year` (año o null), `source_hint`, `decimal_places` (0–6).

```sh
python3 scripts/validate_questions.py
python3 scripts/generate_database.py
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

Versiona JSON y SQLite juntos. Se exige ≥400 preguntas, ≥8 categorías, ≥25 por categoría, esquema exacto, números finitos, rangos válidos e IDs/textos únicos. Las escalas logarítmicas requieren mínimo positivo. Usa rangos amplios; los años usan escala lineal.

## Política editorial

Preguntas reconocibles de cultura general: océanos, montañas, anatomía, historia, animales y vida diaria. Conservar la cifra de referencia con su precisión publicada: **Everest 8.848,86 m (2020), profundidad media oceánica 3.682 m (NOAA)**. No redondear deliberadamente para facilitar aciertos ni inventar decimales para aparentar exactitud. Una media biológica o estimación científica conserva su contexto e incertidumbre; no se convierte en una constante universal. Si hay varias convenciones, explicitar una o descartar la pregunta. No incluir opiniones, pistas que revelen la respuesta ni cálculos tautológicos.

Los conteos usan `decimal_places: 0`; las medidas admiten los decimales necesarios de la referencia, hasta seis. Respuesta y extremos deben ser representables con esa precisión. Selector, teclado y confirmación aplican la misma restricción. El arrastre usa pasos cómodos sin cambiar la respuesta real; el teclado permite precisión completa.

`source_hint` orienta una revisión humana: no equivale a una verificación automatizada en vivo. Añade una institución/documento concreto y fecha cuando corresponda. Véase [fuentes y límites de revisión](docs/SOURCES.md).

## Migraciones

Esquema Room 3, catálogo activo `catalog_version=2`. Las instalaciones nuevas reciben solo las 400 preguntas nuevas. La migración 2→3 archiva las 430 anteriores como versión 1 y añade el catálogo nuevo, conservando las partidas guardadas. El DAO selecciona solo versión 2 para nuevas partidas; busca IDs antiguos únicamente para continuar una partida existente.

`migration_1_2.sql` y `migration_2_3.sql` son históricos **inmutables**. No regenerarlos ni editar sus filas en futuras versiones. El generador actual produce el asset fresco, no actualiza instalaciones existentes. Cada futura edición del catálogo o del esquema distribuido debe incluir su propia migración y probar instalaciones nuevas y actualizadas. Conservar el nombre de la DB instalada para que Room ejecute la cadena de migraciones; no usar migración destructiva.
