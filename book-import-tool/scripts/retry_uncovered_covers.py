from __future__ import annotations

import csv
import json
import re
import subprocess
import sys
import time
from pathlib import Path
from typing import Any

import requests

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = Path(__file__).resolve().parents[2]
COVER_DIR = ROOT / 'backend' / 'uploads' / 'book-covers'
STATE_FILE = ROOT / 'book-import-tool' / 'data' / 'cover_retry_state.json'
LOG_FILE = ROOT / 'logs' / 'cover-retry.log'
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
REQUEST_DELAY_SECONDS = 0.6
MAX_RETRIES = 3
TARGET_COVERAGE = 0.95
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


def initial_state() -> dict[str, Any]:
    return {
        'processed': 0,
        'matched': 0,
        'updated': 0,
        'failed': 0,
        'skipped': 0,
        'index': 0,
        'candidateTotal': 0,
        'coverage': 0.0,
    }


def read_state() -> dict[str, Any]:
    if STATE_FILE.exists():
        return json.loads(STATE_FILE.read_text(encoding='utf-8'))
    return initial_state()


def write_state(state: dict[str, Any]) -> None:
    STATE_FILE.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding='utf-8')


def run_sql(sql: str, timeout: int = 120) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ['java', '-cp', H2_JAR, 'org.h2.tools.Shell', '-url', DB_URL, '-user', 'sa', '-password', '', '-sql', sql],
        cwd=str(ROOT / 'backend'),
        text=True,
        capture_output=True,
        timeout=timeout,
    )


def export_retry_candidates() -> list[dict[str, Any]]:
    out = ROOT / 'book-import-tool' / 'data' / 'books_for_cover_retry.csv'
    sql = (
        "CALL CSVWRITE('"
        + str(out).replace('\\', '/')
        + "', 'SELECT id,title,author,publish_year,cover_url FROM books "
          "WHERE cover_url IS NULL OR cover_url NOT LIKE ''/book-covers/douban-%'' ORDER BY id', "
          "'charset=UTF-8');"
    )
    result = run_sql(sql)
    if result.returncode != 0:
        raise RuntimeError(result.stderr or result.stdout)
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


def export_covered_books() -> list[dict[str, Any]]:
    out = ROOT / 'book-import-tool' / 'data' / 'books_with_cover_retry.csv'
    sql = (
        "CALL CSVWRITE('"
        + str(out).replace('\\', '/')
        + "', 'SELECT id,title,author,publish_year,cover_url FROM books "
          "WHERE cover_url LIKE ''/book-covers/douban-%'' ORDER BY id', "
          "'charset=UTF-8');"
    )
    result = run_sql(sql)
    if result.returncode != 0:
        raise RuntimeError(result.stderr or result.stdout)
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


def query_cover_stats() -> tuple[int, int]:
    sql = (
        "CALL CSVWRITE('"
        + str((ROOT / 'book-import-tool' / 'data' / 'cover_retry_stats.csv')).replace('\\', '/')
        + "', 'SELECT "
          "(SELECT COUNT(*) FROM books) AS total_count, "
          "(SELECT COUNT(*) FROM books WHERE cover_url LIKE ''/book-covers/douban-%'') AS covered_count', "
          "'charset=UTF-8');"
    )
    result = run_sql(sql)
    if result.returncode != 0:
        raise RuntimeError(result.stderr or result.stdout)
    stats_path = ROOT / 'book-import-tool' / 'data' / 'cover_retry_stats.csv'
    with stats_path.open('r', encoding='utf-8', newline='') as handle:
        row = next(csv.DictReader(handle))
        return int(row['TOTAL_COUNT']), int(row['COVERED_COUNT'])


def clean_title(title: str | None) -> str:
    return re.sub(r'["“”《》（）()【】\[\]\s:：·•,，.。\-—–_/\\|!?！？\'"]', '', title or '').lower()


def normalize_title_variants(title: str) -> list[str]:
    variants: list[str] = []
    for fragment in title_fragments(title):
        stripped = fragment.strip()
        if not stripped:
            continue
        variants.append(stripped)
        variants.append(re.sub(r'\s+', ' ', stripped).strip())
        variants.append(strip_title_suffixes(stripped))

    deduped: list[str] = []
    seen: set[str] = set()
    for item in variants:
        value = item.strip()
        if value and value not in seen:
            deduped.append(value)
            seen.add(value)
    return deduped


def strip_title_suffixes(title: str) -> str:
    value = title.strip().strip('.,，。:：-—–· ')
    suffix_patterns = [
        re.compile(r'(?:,?\s*(?:(?:[A-Za-z0-9]+|[ivx]{1,6})\s+edition|edition|volume\s+[A-Za-z0-9ivx]+|vol\.?\s*[A-Za-z0-9ivx]+))$', re.I),
        re.compile(r'(?:第?[0-9一二三四五六七八九十]+(?:版|卷|册))$'),
        re.compile(r'(?:修订版|升级版|典藏版|纪念版|珍藏版|完整版|增订版|上下册|上册|下册|全[二三四五六七八九十0-9]+册|附光盘|附盘)$'),
    ]
    changed = True
    while value and changed:
        changed = False
        for pattern in suffix_patterns:
            stripped = pattern.sub('', value).strip().strip('.,，。:：-—–· ')
            if stripped and stripped != value:
                value = stripped
                changed = True
    return value


def title_fragments(title: str) -> list[str]:
    value = str(title or '').strip()
    if not value:
        return []

    variants = [value]
    variants.append(re.sub(r'^["“”《》]+|["“”《》]+$', '', value))
    variants.append(re.sub(r'^\.', '', value))
    variants.append(re.sub(r'\s*[（(][^）)]+[）)]\s*$', '', value))

    for separator in [':', '：', '-', '—', '–', '|', '/', '·']:
        if separator in value:
            variants.extend(part.strip() for part in value.split(separator) if part.strip())

    variants.extend(part.strip() for part in re.findall(r'[（(]([^）)]+)[）)]', value) if part.strip())

    latin_tokens = re.findall(r"[A-Za-z][A-Za-z0-9+#.&']*", value)
    if latin_tokens:
        variants.append(' '.join(latin_tokens))

    cjk_only = ''.join(ch for ch in value if '\u4e00' <= ch <= '\u9fff')
    if len(cjk_only) >= 4:
        variants.append(cjk_only)

    return variants


def title_keys(title: str | None) -> list[str]:
    deduped: list[str] = []
    seen: set[str] = set()
    for variant in normalize_title_variants(str(title or '')):
        key = clean_title(variant)
        if len(key) >= 2 and key not in seen:
            deduped.append(key)
            seen.add(key)
    return deduped


def author_head(author: str | None) -> str:
    return str(author or '').split('/')[0].strip().lower()


def build_local_cover_index(covered_books: list[dict[str, Any]]) -> tuple[dict[tuple[str, str], str], dict[str, str]]:
    title_author_index: dict[tuple[str, str], str] = {}
    title_index: dict[str, str] = {}
    for book in covered_books:
        cover_url = str(book.get('coverUrl') or '')
        if not cover_url.startswith('/book-covers/douban-'):
            continue
        normalized_author = author_head(book.get('author'))
        for key in title_keys(book.get('title')):
            if normalized_author and (key, normalized_author) not in title_author_index:
                title_author_index[(key, normalized_author)] = cover_url
            if key not in title_index:
                title_index[key] = cover_url
    return title_author_index, title_index


def find_local_cover(
    book: dict[str, Any],
    title_author_index: dict[tuple[str, str], str],
    title_index: dict[str, str],
) -> str:
    normalized_author = author_head(book.get('author'))
    for key in title_keys(book.get('title')):
        if normalized_author:
            direct = title_author_index.get((key, normalized_author))
            if direct:
                return direct
        direct = title_index.get(key)
        if direct:
            return direct
    return ''


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
                time.sleep(1.2 * (attempt + 1))
        time.sleep(0.2)
    return []


def extract_window_data(page_text: str) -> dict[str, Any]:
    marker = 'window.__DATA__ = '
    start = page_text.find(marker)
    if start == -1:
        return {}
    start += len(marker)
    while start < len(page_text) and page_text[start].isspace():
        start += 1
    if start >= len(page_text) or page_text[start] != '{':
        return {}

    depth = 0
    in_string = False
    escaped = False
    end = -1
    for index in range(start, len(page_text)):
        char = page_text[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == '\\':
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == '{':
            depth += 1
        elif char == '}':
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end == -1:
        return {}
    try:
        return json.loads(page_text[start:end])
    except json.JSONDecodeError:
        return {}


def extract_year(text: str) -> str:
    match = re.search(r'\b(19|20)\d{2}\b', text or '')
    return match.group(0) if match else ''


def parse_search_subject_items(page_text: str) -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    data = extract_window_data(page_text)
    for item in data.get('items', []):
        if item.get('tpl_name') != 'search_subject':
            continue
        abstract = str(item.get('abstract') or '')
        abstract_2 = str(item.get('abstract_2') or '')
        items.append(
            {
                'id': str(item.get('id') or ''),
                'title': str(item.get('title') or ''),
                'author_name': abstract or abstract_2,
                'year': extract_year(abstract_2 or abstract),
                'pic': str(item.get('cover_url') or ''),
            }
        )
    return items


def search_subjects(session: requests.Session, title: str) -> list[dict[str, Any]]:
    for query in normalize_title_variants(title):
        for attempt in range(MAX_RETRIES):
            try:
                response = session.get(
                    'https://search.douban.com/book/subject_search',
                    params={'search_text': query},
                    timeout=20,
                )
                if response.ok:
                    items = parse_search_subject_items(response.text)
                    if items:
                        return items
                break
            except Exception as exc:
                if attempt == MAX_RETRIES - 1:
                    log(f'SEARCH_ERROR title={title!r} query={query!r} error={exc!r}')
                time.sleep(1.2 * (attempt + 1))
        time.sleep(0.2)
    return []


def score_title_match(book_title: str | None, item_title: str | None) -> int:
    best = 0
    for book_key in title_keys(book_title):
        for item_key in title_keys(item_title):
            if book_key == item_key:
                return 100
            shorter, longer = (book_key, item_key) if len(book_key) <= len(item_key) else (item_key, book_key)
            if len(shorter) < 4 or shorter not in longer:
                continue
            ratio = len(shorter) / len(longer)
            if ratio >= 0.8:
                best = max(best, 80)
            elif ratio >= 0.6:
                best = max(best, 60)
            elif ratio >= 0.45:
                best = max(best, 40)
    return best


def choose_match(book: dict[str, Any], items: list[dict[str, Any]], min_score: int = 10) -> dict[str, Any] | None:
    normalized_author = author_head(book.get('author'))
    book_year = str(book.get('year') or '')
    best: dict[str, Any] | None = None
    best_score = -1
    for item in items:
        score = 0
        title_score = score_title_match(book.get('title'), item.get('title'))
        author_matched = False
        year_matched = False

        score += title_score
        author_name = str(item.get('author_name') or '').lower()
        if normalized_author and normalized_author in author_name:
            author_matched = True
            score += 25
        if book_year and book_year == str(item.get('year') or '')[:4]:
            year_matched = True
            score += 10
        if item.get('pic'):
            score += 10
        if not (title_score > 0 or (author_matched and year_matched)):
            continue
        if score > best_score:
            best = item
            best_score = score
    return best if best and best_score >= min_score else None


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


def update_book_cover(book_id: int, cover_url: str) -> bool:
    sql = f"UPDATE books SET cover_url = '{sql_escape(cover_url)}', updated_at = CURRENT_TIMESTAMP WHERE id = {book_id};"
    result = run_sql(sql, timeout=30)
    if result.returncode != 0:
        log(f'DB_UPDATE_FAILED id={book_id} stderr={result.stderr[:240]} stdout={result.stdout[:240]}')
        return False
    return True


def parse_args(argv: list[str]) -> tuple[int, bool]:
    limit = 0
    restart = False
    for arg in argv:
        if arg == '--restart':
            restart = True
            continue
        limit = int(arg)
    return limit, restart


def main() -> int:
    limit, restart = parse_args(sys.argv[1:])
    state = initial_state() if restart else read_state()
    if restart:
        write_state(state)
        log('RESTART state reset requested by --restart')
    total_count, covered_count = query_cover_stats()
    current_coverage = (covered_count / total_count) if total_count else 0.0
    state['coverage'] = current_coverage
    if current_coverage >= TARGET_COVERAGE:
        write_state(state)
        log(f'FINISH coverage reached {current_coverage:.4f}')
        return 0

    candidates = export_retry_candidates()
    covered_books = export_covered_books()
    title_author_index, title_index = build_local_cover_index(covered_books)
    state['candidateTotal'] = len(candidates)
    start_index = int(state.get('index', 0))
    session = douban_session()
    processed_this_run = 0
    log(
        f"START index={start_index} candidates={len(candidates)} "
        f"covered={covered_count}/{total_count} coverage={current_coverage:.4f} limit={limit or 'all'}"
    )

    for index in range(start_index, len(candidates)):
        book = candidates[index]
        if limit and processed_this_run >= limit:
            state['index'] = index
            total_count, covered_count = query_cover_stats()
            state['coverage'] = (covered_count / total_count) if total_count else 0.0
            write_state(state)
            log(f"STOP limit reached processed_this_run={processed_this_run} state={state}")
            return 0

        state['processed'] += 1
        processed_this_run += 1
        state['index'] = index + 1

        local_cover = find_local_cover(book, title_author_index, title_index)
        if local_cover:
            state['matched'] += 1
            if update_book_cover(int(book['id']), local_cover):
                state['updated'] += 1
                log(f"UPDATED_LOCAL id={book['id']} title={book.get('title')!r} cover={local_cover}")
            else:
                state['failed'] += 1
            total_count, covered_count = query_cover_stats()
            state['coverage'] = (covered_count / total_count) if total_count else 0.0
            write_state(state)
            if state['coverage'] >= TARGET_COVERAGE:
                log(f"FINISH coverage reached {state['coverage']:.4f}")
                return 0
            time.sleep(REQUEST_DELAY_SECONDS)
            continue

        subject_items = suggest(session, str(book.get('title') or ''))
        match = choose_match(book, subject_items)
        if not match:
            match = choose_match(book, search_subjects(session, str(book.get('title') or '')), min_score=35)
        if not match:
            state['failed'] += 1
            log(f"NO_MATCH id={book['id']} title={book.get('title')!r}")
            write_state(state)
            continue

        state['matched'] += 1
        cover_url = download_cover(session, int(book['id']), match)
        if not cover_url:
            state['failed'] += 1
            log(f"DOWNLOAD_FAILED id={book['id']} title={book.get('title')!r} douban={match.get('id')}")
            write_state(state)
            continue

        if update_book_cover(int(book['id']), cover_url):
            state['updated'] += 1
            log(f"UPDATED id={book['id']} title={book.get('title')!r} douban={match.get('id')} cover={cover_url}")
        else:
            state['failed'] += 1

        total_count, covered_count = query_cover_stats()
        state['coverage'] = (covered_count / total_count) if total_count else 0.0
        write_state(state)
        if state['coverage'] >= TARGET_COVERAGE:
            log(f"FINISH coverage reached {state['coverage']:.4f}")
            return 0
        time.sleep(REQUEST_DELAY_SECONDS)

    total_count, covered_count = query_cover_stats()
    state['coverage'] = (covered_count / total_count) if total_count else 0.0
    write_state(state)
    log(f"FINISH exhausted candidates state={state}")
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
