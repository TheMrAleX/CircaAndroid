# Preguntas offline

Hay 430 preguntas en 12 categorías (412 enteras y 18 medidas con decimales), con al menos 30 por categoría. Los JSON son la fuente de verdad; `questions.db` es un asset SQLite generado, abierto mediante Room con `createFromAsset`.

Para añadir datos, pega objetos en `data/questions/<categoria>.json` como los existentes. Campos: `id` único y estable, `category`, `text_es` en español, `answer` numérico, `unit` (texto o null), `min_plausible`, `max_plausible`, `scale` (`linear`/`log`), `difficulty` (1–3), `as_of_year` (año o null), `source_hint`, `decimal_places` (0 para enteros; 1–3 solo cuando la medida necesita fracciones).

```sh
python3 scripts/validate_questions.py
python3 scripts/generate_database.py
./gradlew :core:test :app:assembleDebug
```

Versiona JSON y SQLite juntos. El validador exige ≥400 preguntas, ≥8 categorías, ≥25 por categoría, campos exactos, números finitos, rangos válidos e IDs/textos únicos. Escalas logarítmicas requieren mínimo positivo. Los rangos son pistas amplias deliberadas; los años usan escala lineal. Si cambias columnas, tipos o datos de una app distribuida, aumenta la versión Room y añade una migración. El generador actual produce esquema 2. `migration_1_2.sql` es una migración histórica inmutable: no modificarla para futuras versiones. Se conserva el nombre del archivo instalado para que Room migre la DB existente.

Prefiere hechos estables, define unidad y contexto. Las cifras redondeadas se preguntan como aproximaciones. Las cifras políticas se fijan a un cambio histórico explícito; no se incluyen récords actuales sin año. `source_hint` orienta la revisión, no representa una verificación individual automatizada. Véase `docs/SOURCES.md` para el muestreo externo. No añadas opiniones, cálculos tautológicos, cifras disputadas sin convención o datos personales.

La precisión es una decisión editorial independiente de la respuesta: por defecto usa `decimal_places: 0`, también en medidas redondeadas a unidades enteras. Reserva 1–3 decimales para dimensiones o conversiones que necesiten esa precisión. El validador exige que respuesta y extremos sean representables con la precisión declarada. El selector, teclado y confirmación comparten esta restricción.
