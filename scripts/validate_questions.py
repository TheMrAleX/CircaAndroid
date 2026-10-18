#!/usr/bin/env python3
"""Strict validation shared by generation and CI; no network required."""
from decimal import Decimal
import json
import math
import unicodedata
from collections import Counter
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
FIELDS = {'id','category','text_es','answer','unit','min_plausible','max_plausible','scale','difficulty','as_of_year','source_hint','decimal_places'}
def load_questions():
    questions, ids, texts = [], set(), set()
    for path in sorted((ROOT/'data/questions').glob('*.json')):
        rows = json.loads(path.read_text())
        assert isinstance(rows, list) and rows, f'{path}: empty/non-array'
        for q in rows:
            assert set(q) == FIELDS, f'{path}: schema: {q}'
            assert isinstance(q['id'],str) and q['id'] and q['id'] not in ids, f'duplicate id {q["id"]}'
            ids.add(q['id'])
            for key in ('category','text_es','source_hint'):
                assert isinstance(q[key],str) and q[key].strip(), f'{q["id"]}: {key}'
            assert q['text_es'].startswith('¿') and q['text_es'].endswith('?'), q['id']
            norm = ' '.join(unicodedata.normalize('NFKC',q['text_es']).casefold().split())
            assert norm not in texts, f'duplicate text: {norm}'
            texts.add(norm)
            for key in ('answer','min_plausible','max_plausible'):
                assert type(q[key]) in (int,float) and math.isfinite(q[key]), f'{q["id"]}: {key}'
            assert 0 <= q['min_plausible'] <= q['answer'] <= q['max_plausible'], q['id']
            assert q['min_plausible'] < q['max_plausible'], q['id']
            assert q['scale'] in ('linear','log'), q['id']
            assert q['scale'] != 'log' or q['min_plausible'] > 0, q['id']
            assert type(q['difficulty']) is int and q['difficulty'] in (1,2,3), q['id']
            assert q['as_of_year'] is None or (type(q['as_of_year']) is int and 1 <= q['as_of_year'] <= 2100), q['id']
            assert q['unit'] is None or isinstance(q['unit'],str), q['id']
            assert type(q['decimal_places']) is int and 0 <= q['decimal_places'] <= 6, q['id']
            for key in ('answer','min_plausible','max_plausible'):
                assert Decimal(str(q[key])).normalize().as_tuple().exponent >= -q['decimal_places'], f'{q["id"]}: {key} exceeds decimal_places'
            questions.append(q)
    counts = Counter(q['category'] for q in questions)
    assert len(questions) >= 400, f'Only {len(questions)} questions'
    assert len(counts) >= 8 and min(counts.values()) >= 25, counts
    return questions
if __name__ == '__main__':
    q = load_questions()
    print(f'OK: {len(q)} preguntas; {dict(sorted(Counter(x["category"] for x in q).items()))}')
