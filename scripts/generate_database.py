#!/usr/bin/env python3
"""Build deterministic Room-compatible SQLite asset from validated JSON."""
import sqlite3
import argparse
from validate_questions import ROOT, load_questions

def generate():
    qs = load_questions()
    path = ROOT/'app/src/main/assets/questions.db'
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists(): path.unlink()
    db = sqlite3.connect(path)
    db.execute('PRAGMA user_version=4')
    db.execute('''CREATE TABLE IF NOT EXISTS questions (
      id TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL, text_es TEXT NOT NULL,
      answer REAL NOT NULL, unit TEXT, min_plausible REAL NOT NULL,
      max_plausible REAL NOT NULL, scale TEXT NOT NULL, difficulty INTEGER NOT NULL,
      as_of_year INTEGER, source_hint TEXT NOT NULL, decimal_places INTEGER NOT NULL DEFAULT 0, catalog_version INTEGER NOT NULL DEFAULT 1)''')
    keys = ['id','category','text_es','answer','unit','min_plausible','max_plausible','scale','difficulty','as_of_year','source_hint','decimal_places']
    db.executemany('INSERT INTO questions VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)',[[q[k] for k in keys]+[2] for q in qs])
    db.commit()
    assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    assert db.execute('SELECT COUNT(*) FROM questions').fetchone()[0] == len(qs)
    db.execute('VACUUM')
    db.close()
    print(f'SQLite: {len(qs)} preguntas → {path.relative_to(ROOT)}')
def migration():
    rows=[q for q in load_questions() if q['id'].startswith('v109_')]
    assert len(rows)==400, 'La migración 3→4 debe contener exactamente las 400 altas de 1.0.9'
    path=ROOT/'app/src/main/assets/migration_3_4.sql'
    assert not path.exists(), 'Migración congelada: no sobrescribir una migración ya creada'
    db=sqlite3.connect(':memory:')
    keys=['id','category','text_es','answer','unit','min_plausible','max_plausible','scale','difficulty','as_of_year','source_hint','decimal_places']
    lines=[]
    for q in rows:
        values=[q[k] for k in keys]+[2]
        quoted=[db.execute('SELECT quote(?)',(value,)).fetchone()[0] for value in values]
        lines.append('INSERT OR IGNORE INTO questions ('+','.join(keys+['catalog_version'])+') VALUES ('+','.join(quoted)+');')
    path.write_text('\n'.join(lines)+'\n')
    db.close()
    print('Migración inmutable creada:',path.relative_to(ROOT))
if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--write-migration-3-4',action='store_true')
    args=parser.parse_args()
    if args.write_migration_3_4: migration()
    generate()
