#!/usr/bin/env python3
"""Build deterministic Room-compatible SQLite asset from validated JSON."""
import sqlite3
from validate_questions import ROOT, load_questions

def generate():
    qs = load_questions()
    path = ROOT/'app/src/main/assets/questions.db'
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists(): path.unlink()
    db = sqlite3.connect(path)
    db.execute('PRAGMA user_version=3')
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
if __name__ == '__main__': generate()
