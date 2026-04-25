from __future__ import annotations

import json
import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
import time
from pathlib import Path
from typing import Any

import requests
import subprocess

ROOT = Path(__file__).resolve().parents[2]
COVER_DIR = ROOT / 'backend' / 'uploads' / 'book-covers'
STATE_FILE = ROOT / 'book-import-tool' / 'data' / 'cover_import_state.json'
LOG_FILE = ROOT / 'logs' / 'cover-import.log'
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
REQUEST_DELAY_SECONDS = 0.8
MAX_RETRIES = 3
H2_JAR = str(Path.home() / '.m2' / 'repository' / 'com' / 'h2database' / 'h2' / '2.2.224' / 'h2-2.2.224.jar')
DB_URL = f'jdbc:h2:file:{ROOT / "backend" / "data" / "library-dev"};MODE=MySQL;NON_KEYWORDS=YEAR'

COVER_DIR.mkdir(parents=True, exist_ok=True)
STATE_FILE.parent.mkdir(parents=True, exist_ok=True)
LOG_FILE.parent.mkdir(parents=True, exist_ok=True)


def log(message: str) -> None:
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')} {message}"
    print(line, flush=True)
    with LOG_FILE.open('a', encoding='utf-8') as handle:
        handle.write(line + '\n')


def read_state() -> dict[str, Any]:
    if STATE_FILE.exists():
        return json.loads(STATE_FILE.read_text(encoding='utf-8'))
    return {'page': 0, 'processed': 0, 'matched': 0, 'updated': 0, 'failed': 0, 'skipped': 0}


def write_state(state: dict[str, Any]) -> None:
    STATE_FILE.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding='utf-8')


def clean_title(title: str | None) -> str:
    return re.sub(r'["“”《》（）()\s:：·•,，.。-]', '', title or '').lower()


def normalize_title_variants(title: str) -> list[str]:
    variants = [
        title,
        re.sub(r'^["“”]+|["“”]+$', '', title),
        re.sub(r'^\.', '', title),
        re.sub(r'\s+', '', title),
        re.sub(r'\s*\([^)]*\)\s*$', '', title),
        re.sub(r'（[^）]*）\s*$', '', title),
    ]
    deduped: list[str] = []
    seen: set[str] = set()
    for item in variants:
        value = item.strip()
        if value and value not in seen:
            deduped.append(value)
            seen.add(value)
    return deduped


def douban_session() -> requests.Session:
    session = requests.Session()
    session.headers.update({'User-Agent': UA, 'Referer': 'https://book.douban.com/'})
    return session


def suggest(session: requests.Session, title: str) -> list[dict[str, Any]]:
    for query in normalize_title_variants(title):
        for attempt in range(MAX_RETRIES):
            try:
                response = session.get(
                    'https://book.douban.com/j/subject_suggest',
                    params={'q': query},
                    timeout=20,
                )
                if response.ok:
                    items = response.json()
                    if items:
                        return items
                break
            except Exception as exc:
                if attempt == MAX_RETRIES - 1:
                    log(f'SUGGEST_ERROR title={title!r} query={query!r} error={exc!r}')
                time.sleep(1.5 * (attempt + 1))
        time.sleep(REQUEST_DELAY_SECONDS)
    return []


def choose_match(book: dict[str, Any], items: list[dict[str, Any]]) -> dict[str, Any] | None:
    book_title = clean_title(book.get('title'))
    author_head = str(book.get('author') or '').split('/')[0].strip()
    best: dict[str, Any] | None = None
    best_score = -1
    for item in items:
        item_title = clean_title(item.get('title'))
        score = 0
        if item_title == book_title:
            score += 100
        elif item_title and (item_title in book_title or book_title in item_title):
            score += 60
        if author_head and author_head in str(item.get('author_name') or ''):
            score += 25
        if str(book.get('year') or '') and str(book.get('year')) == str(item.get('year') or '')[:4]:
            score += 10
        if item.get('pic'):
            score += 10
        if score > best_score:
            best = item
            best_score = score
    return best if best and best_score >= 10 else None


def candidate_cover_urls(pic: str | None) -> list[str]:
    if not pic:
        return []
    normalized = pic.replace('\\/', '/')
    size_urls = []
    for size in ['l', 'm', 's']:
        size_urls.append(re.sub(r'/[sml]/public/', f'/{size}/public/', normalized))
    all_urls: list[str] = []
    for url in size_urls:
        all_urls.append(url)
        for host in ['img1.doubanio.com', 'img2.doubanio.com', 'img3.doubanio.com', 'img9.doubanio.com']:
            all_urls.append(re.sub(r'img\d\.doubanio\.com', host, url))
    return list(dict.fromkeys(all_urls))


def download_cover(session: requests.Session, book_id: int, match: dict[str, Any]) -> str:
    headers = {
        'User-Agent': UA,
        'Referer': f"https://book.douban.com/subject/{match.get('id', '')}/",
        'Accept': 'image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8',
    }
    for url in candidate_cover_urls(match.get('pic')):
        try:
            response = session.get(url, headers=headers, timeout=25)
            if response.ok and 'image' in response.headers.get('Content-Type', '').lower() and len(response.content) > 1000:
                path = COVER_DIR / f'douban-{book_id}.jpg'
                path.write_bytes(response.content)
                return f'/book-covers/douban-{book_id}.jpg'
        except Exception:
            continue
    return ''


def sql_escape(value: str) -> str:
    return value.replace("'", "''")


def update_book_cover_db(_conn: object, book_id: int, cover_url: str) -> bool:
    sql = f"UPDATE books SET cover_url = '{sql_escape(cover_url)}', updated_at = CURRENT_TIMESTAMP WHERE id = {book_id};"
    result = subprocess.run(
        [
            'java', '-cp', H2_JAR, 'org.h2.tools.Shell',
            '-url', DB_URL,
            '-user', 'sa', '-password', '', '-sql', sql,
        ],
        cwd=str(ROOT / 'backend'),
        text=True,
        capture_output=True,
        timeout=30,
    )
    if result.returncode != 0:
        log(f'DB_UPDATE_FAILED id={book_id} stderr={result.stderr[:240]} stdout={result.stdout[:240]}')
        return False
    return True

def has_real_cover(book: dict[str, Any]) -> bool:
    return str(book.get('coverUrl') or '').startswith('/book-covers/douban-')


def export_books_from_db() -> list[dict[str, Any]]:
    out = ROOT / 'book-import-tool' / 'data' / 'books_for_cover_import.csv'
    sql = (
        "CALL CSVWRITE('"
        + str(out).replace('\\', '/')
        + "', 'SELECT id,title,author,publish_year,cover_url FROM books ORDER BY id', 'charset=UTF-8');"
    )
    result = subprocess.run(
        ['java', '-cp', H2_JAR, 'org.h2.tools.Shell', '-url', DB_URL, '-user', 'sa', '-password', '', '-sql', sql],
        cwd=str(ROOT / 'backend'), text=True, capture_output=True, timeout=120,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr or result.stdout)
    import csv
    with out.open('r', encoding='utf-8', newline='') as handle:
        return [
            {
                'id': int(row['ID']),
                'title': row['TITLE'],
                'author': row['AUTHOR'],
                'year': row.get('PUBLISH_YEAR') or '',
                'coverUrl': row.get('COVER_URL') or '',
            }
            for row in csv.DictReader(handle)
        ]


def main() -> int:
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else 0
    state = read_state()
    douban = douban_session()
    db_conn = object()
    processed_this_run = 0
    all_books = export_books_from_db()
    start_index = int(state.get('index', 0))
    log(f"START index={start_index} total={len(all_books)} limit={limit or 'all'}")

    for index in range(start_index, len(all_books)):
        book = all_books[index]
        if limit and processed_this_run >= limit:
            state['index'] = index
            write_state(state)
            log(f"STOP limit reached processed_this_run={processed_this_run} state={state}")
            return 0

        state['processed'] += 1
        processed_this_run += 1
        state['index'] = index + 1

        if has_real_cover(book):
            state['skipped'] += 1
            write_state(state)
            continue

        match = choose_match(book, suggest(douban, str(book.get('title') or '')))
        if not match:
            state['failed'] += 1
            log(f"NO_MATCH id={book['id']} title={book.get('title')!r}")
            write_state(state)
            continue

        state['matched'] += 1
        cover_url = download_cover(douban, int(book['id']), match)
        if not cover_url:
            state['failed'] += 1
            log(f"DOWNLOAD_FAILED id={book['id']} title={book.get('title')!r} douban={match.get('id')}")
            write_state(state)
            continue

        if update_book_cover_db(db_conn, int(book['id']), cover_url):
            state['updated'] += 1
            log(f"UPDATED id={book['id']} title={book.get('title')!r} douban={match.get('id')} cover={cover_url}")
        else:
            state['failed'] += 1

        write_state(state)
        time.sleep(REQUEST_DELAY_SECONDS)

    write_state(state)
    log(f"FINISH state={state}")
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
