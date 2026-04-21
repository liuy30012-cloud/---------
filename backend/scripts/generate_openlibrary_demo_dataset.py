#!/usr/bin/env python3
"""Generate a demo CSV dataset from official Open Library dumps."""

from __future__ import annotations

import argparse
import csv
import gzip
import json
import logging
import re
import sys
import urllib.request
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Iterator, TextIO

EDITIONS_URL = "https://openlibrary.org/data/ol_dump_editions_latest.txt.gz"
AUTHORS_URL = "https://openlibrary.org/data/ol_dump_authors_latest.txt.gz"
TARGET_HEADERS = [
    "title",
    "author",
    "isbn",
    "location",
    "coverUrl",
    "status",
    "year",
    "description",
    "languageCode",
    "availability",
    "category",
    "circulationPolicy",
    "totalCopies",
]

YEAR_PATTERN = re.compile(r"(19|20)\d{2}")
HAN_PATTERN = re.compile(r"[\u3400-\u9fff]")

CATEGORY_RULES: list[tuple[str, tuple[str, ...]]] = [
    ("计算机", ("computer", "software", "program", "database", "algorithm", "cyber", "network", "machine learning")),
    ("文学", ("novel", "poetry", "fiction", "literature", "drama", "story")),
    ("历史", ("history", "histor", "war", "ancient", "archive")),
    ("艺术设计", ("art", "design", "music", "painting", "visual")),
    ("经济管理", ("business", "management", "finance", "leadership", "economics")),
    ("社会科学", ("sociology", "society", "politics", "law", "culture", "library")),
    ("教育", ("education", "teaching", "learning", "school")),
    ("哲学", ("philosophy", "ethics", "logic", "religion")),
    ("语言学习", ("language", "linguistics", "grammar", "translation")),
    ("儿童读物", ("children", "juvenile", "kids", "young adult")),
    ("科学技术", ("science", "physics", "chemistry", "biology", "statistics", "mathematics")),
]

LANGUAGE_ALIASES = {
    "eng": "en",
    "en": "en",
    "chi": "zh",
    "zho": "zh",
    "zh": "zh",
    "chn": "zh",
}


@dataclass
class CandidateBook:
    title: str
    isbn: str
    author: str | None
    author_keys: list[str]
    year: str
    description: str
    language_code: str
    category: str
    cover_url: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--editions-url", default=EDITIONS_URL)
    parser.add_argument("--authors-url", default=AUTHORS_URL)
    parser.add_argument("--output", required=True, help="Output .csv.gz path")
    parser.add_argument("--report", help="Optional JSON report path")
    parser.add_argument("--target-count", type=int, default=100_000)
    parser.add_argument("--scan-limit", type=int, default=3_000_000)
    parser.add_argument("--zh-ratio", type=float, default=0.30)
    parser.add_argument("--log-interval", type=int, default=50_000)
    return parser.parse_args()


def open_gzip_text(source: str) -> Iterator[str]:
    if source.startswith("http://") or source.startswith("https://"):
        response = urllib.request.urlopen(source, timeout=60)
        gzip_stream = gzip.GzipFile(fileobj=response)
        text_stream = gzip_stream
    else:
        gzip_stream = gzip.open(source, "rb")
        text_stream = gzip_stream

    try:
        for raw_line in text_stream:
            yield raw_line.decode("utf-8", "replace")
    finally:
        gzip_stream.close()
        if source.startswith("http://") or source.startswith("https://"):
            response.close()


def iter_dump_records(source: str) -> Iterator[dict]:
    for line in open_gzip_text(source):
        parts = line.rstrip("\n").split("\t", 4)
        if len(parts) < 5:
            continue
        try:
            yield json.loads(parts[4])
        except json.JSONDecodeError:
            continue


def normalize_isbn(raw: str | None) -> str:
    if not raw:
        return ""
    normalized = "".join(char for char in raw.upper() if char.isdigit() or char == "X")
    return normalized


def extract_isbn(record: dict) -> str:
    for key in ("isbn_13", "isbn_10"):
        values = record.get(key) or []
        for value in values:
            normalized = normalize_isbn(value)
            if normalized:
                return normalized
    return ""


def extract_author_keys(record: dict) -> list[str]:
    result: list[str] = []
    for author in record.get("authors") or []:
        key = author.get("key")
        if key:
            result.append(key)
    return result


def extract_author_fallback(record: dict) -> str | None:
    by_statement = str(record.get("by_statement") or "").strip()
    return by_statement or None


def extract_year(record: dict) -> str:
    publish_date = str(record.get("publish_date") or "").strip()
    match = YEAR_PATTERN.search(publish_date)
    return match.group(0) if match else ""


def extract_description(record: dict, title: str) -> str:
    raw_description = record.get("description")
    if isinstance(raw_description, dict):
        text = str(raw_description.get("value") or "").strip()
    else:
        text = str(raw_description or "").strip()

    if text:
        return text[:500]

    subtitle = str(record.get("subtitle") or "").strip()
    if subtitle:
        return f"{title}: {subtitle}"[:500]

    return f"Open Library demo record for {title}."


def extract_language(record: dict) -> str:
    for item in record.get("languages") or []:
        key = str(item.get("key") or "").split("/")[-1].strip().lower()
        if key in LANGUAGE_ALIASES:
            return LANGUAGE_ALIASES[key]

    haystack = " ".join([
        str(record.get("title") or ""),
        str(record.get("subtitle") or ""),
        str(record.get("by_statement") or ""),
    ])
    if HAN_PATTERN.search(haystack):
        return "zh"

    return "en"


def extract_category(record: dict, language_code: str) -> str:
    subjects = [str(subject).strip() for subject in (record.get("subjects") or []) if str(subject).strip()]
    searchable = " ".join(subjects + [str(record.get("title") or ""), str(record.get("subtitle") or "")]).lower()

    for category, keywords in CATEGORY_RULES:
        if any(keyword in searchable for keyword in keywords):
            return category

    return "文学" if language_code == "zh" else "综合"


def extract_cover_url(record: dict) -> str:
    covers = record.get("covers") or []
    if covers:
        return f"https://covers.openlibrary.org/b/id/{covers[0]}-M.jpg?default=false"

    edition_key = str(record.get("key") or "").split("/")[-1].strip()
    if edition_key:
        return f"https://covers.openlibrary.org/b/olid/{edition_key}-M.jpg?default=false"

    return ""


def is_chinese_candidate(record: dict, language_code: str) -> bool:
    if language_code == "zh":
        return True

    subjects = " ".join(str(subject) for subject in (record.get("subjects") or []))
    haystack = " ".join([
        str(record.get("title") or ""),
        str(record.get("subtitle") or ""),
        str(record.get("description") or ""),
        subjects,
    ])
    return bool(HAN_PATTERN.search(haystack))


def build_candidate(record: dict) -> CandidateBook | None:
    title = str(record.get("title") or "").strip()
    if not title:
        return None

    isbn = extract_isbn(record)
    if not isbn:
        return None

    author = extract_author_fallback(record)
    author_keys = extract_author_keys(record)
    if not author and not author_keys:
        return None

    language_code = extract_language(record)
    return CandidateBook(
        title=title,
        isbn=isbn,
        author=author,
        author_keys=author_keys,
        year=extract_year(record),
        description=extract_description(record, title),
        language_code="zh" if is_chinese_candidate(record, language_code) else language_code,
        category=extract_category(record, language_code),
        cover_url=extract_cover_url(record),
    )


def collect_candidates(args: argparse.Namespace) -> tuple[list[CandidateBook], dict]:
    zh_target = int(args.target_count * args.zh_ratio)
    other_target = args.target_count
    zh_books: list[CandidateBook] = []
    other_books: list[CandidateBook] = []
    seen_isbns: set[str] = set()
    processed_rows = 0

    for record in iter_dump_records(args.editions_url):
        processed_rows += 1
        if processed_rows % args.log_interval == 0:
            logging.info(
                "Scanned %s edition rows. collected=%s zh=%s other=%s",
                processed_rows,
                len(zh_books) + len(other_books),
                len(zh_books),
                len(other_books),
            )

        candidate = build_candidate(record)
        if candidate is None or candidate.isbn in seen_isbns:
            if processed_rows >= args.scan_limit and len(other_books) >= args.target_count:
                break
            continue

        seen_isbns.add(candidate.isbn)
        if candidate.language_code == "zh" and len(zh_books) < zh_target:
            zh_books.append(candidate)
        elif len(other_books) < other_target:
            other_books.append(candidate)

        if (
            len(zh_books) >= zh_target
            and len(other_books) >= args.target_count - zh_target
            and len(other_books) >= args.target_count
        ):
            break

        if processed_rows >= args.scan_limit and len(other_books) >= args.target_count:
            break

    final_books = zh_books + other_books[: max(args.target_count - len(zh_books), 0)]
    if len(final_books) < args.target_count:
        final_books.extend(other_books[len(final_books) - len(zh_books): args.target_count - len(zh_books)])

    return final_books[: args.target_count], {
        "processed_rows": processed_rows,
        "zh_target": zh_target,
        "zh_selected": len(zh_books),
        "other_selected": len(other_books),
    }


def resolve_authors(candidates: list[CandidateBook], authors_url: str, log_interval: int) -> dict[str, str]:
    needed_keys = {
        author_key
        for candidate in candidates
        if not candidate.author
        for author_key in candidate.author_keys
    }
    if not needed_keys:
        return {}

    author_names: dict[str, str] = {}
    scanned_rows = 0
    for record in iter_dump_records(authors_url):
        scanned_rows += 1
        if scanned_rows % log_interval == 0:
            logging.info("Scanned %s author rows. resolved=%s/%s", scanned_rows, len(author_names), len(needed_keys))

        key = str(record.get("key") or "").strip()
        if key in needed_keys:
            name = str(record.get("name") or record.get("personal_name") or "").strip()
            if name:
                author_names[key] = name
            if len(author_names) >= len(needed_keys):
                break

    return author_names


def finalize_author(candidate: CandidateBook, author_names: dict[str, str]) -> str:
    if candidate.author:
        return candidate.author

    resolved = [author_names[key] for key in candidate.author_keys if key in author_names]
    if resolved:
        return " / ".join(resolved[:2])

    if candidate.author_keys:
        return candidate.author_keys[0].split("/")[-1]

    return "Unknown Author"


def build_inventory_fields(index: int) -> tuple[str, str, str, int]:
    pattern = index % 20
    if pattern == 19:
        return "REFERENCE_ONLY", "Reading Room Only", "REFERENCE_ONLY", 1
    if pattern >= 16:
        total_copies = 2 + (index % 4)
        status = "CHECKED_OUT" if pattern == 18 else "PARTIALLY_AVAILABLE"
        availability = "Checked Out" if pattern == 18 else "Limited Availability"
        return status, availability, "AUTO", total_copies
    return "AVAILABLE", "Available", "AUTO", 2 + (index % 5)


def build_location(index: int, category: str) -> str:
    zone = ["A区", "B区", "C区", "D区", "E区"][index % 5]
    level = (index % 5) + 1
    shelf = f"{(index % 180) + 1:03d}"
    return f"{zone}>{level}层>{category}>{shelf}"


def write_output(candidates: list[CandidateBook], author_names: dict[str, str], output_path: Path) -> dict:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    category_counts = Counter()
    language_counts = Counter()

    with gzip.open(output_path, "wt", encoding="utf-8", newline="") as output_stream:
        writer = csv.DictWriter(output_stream, fieldnames=TARGET_HEADERS)
        writer.writeheader()

        for index, candidate in enumerate(candidates):
            status, availability, circulation_policy, total_copies = build_inventory_fields(index)
            author = finalize_author(candidate, author_names)
            category_counts[candidate.category] += 1
            language_counts[candidate.language_code] += 1
            writer.writerow({
                "title": candidate.title,
                "author": author,
                "isbn": candidate.isbn,
                "location": build_location(index, candidate.category),
                "coverUrl": candidate.cover_url,
                "status": status,
                "year": candidate.year,
                "description": candidate.description,
                "languageCode": candidate.language_code,
                "availability": availability,
                "category": candidate.category,
                "circulationPolicy": circulation_policy,
                "totalCopies": total_copies,
            })

    return {
        "category_distribution": dict(category_counts.most_common()),
        "language_distribution": dict(language_counts.most_common()),
    }


def write_report(path: Path, report: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> int:
    csv.field_size_limit(sys.maxsize)
    args = parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    candidates, scan_report = collect_candidates(args)
    logging.info("Collected %s candidate books for final output.", len(candidates))

    author_names = resolve_authors(candidates, args.authors_url, args.log_interval)
    output_report = write_output(candidates, author_names, Path(args.output))

    report = {
        "editions_url": args.editions_url,
        "authors_url": args.authors_url,
        "target_count": args.target_count,
        "actual_count": len(candidates),
        "resolved_author_names": len(author_names),
        **scan_report,
        **output_report,
    }

    if args.report:
        write_report(Path(args.report), report)

    logging.info("Wrote %s demo books to %s", len(candidates), args.output)
    if args.report:
        logging.info("Wrote report to %s", args.report)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
