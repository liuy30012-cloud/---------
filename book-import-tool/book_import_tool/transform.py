from __future__ import annotations

import re
from typing import Any

from .settings import Settings


class BookTransformer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def clean_text(self, text: str) -> str:
        if not text:
            return ''
        cleaned = re.sub(r'<[^>]+>', '', text)
        cleaned = re.sub(r'\s+', ' ', cleaned)
        return cleaned.strip()

    def validate_isbn(self, isbn: str) -> bool:
        if not isbn:
            return False
        normalized = isbn.replace('-', '').replace(' ', '')
        return len(normalized) in (10, 13) and normalized.isdigit()

    def extract_year(self, pubdate: str) -> str:
        if not pubdate:
            return ''
        match = re.search(r'(\d{4})', pubdate)
        return match.group(1) if match else ''

    def convert_book(self, book: dict[str, Any]) -> dict[str, Any] | None:
        title = str(book.get('title', '')).strip()
        author = book.get('author', [])
        isbn = book.get('isbn13', '') or book.get('isbn10', '')

        if not title or not author or not isbn or not self.validate_isbn(str(isbn)):
            return None

        author_str = ', '.join(author) if isinstance(author, list) else str(author)
        tags = book.get('tags', [])
        category = tags[0].get('name', '') if tags else ''
        images = book.get('images', {})
        cover_url = images.get('large', '') or images.get('medium', '') or images.get('small', '')

        return {
            'title': self.clean_text(title),
            'author': self.clean_text(author_str),
            'isbn': str(isbn).replace('-', '').replace(' ', ''),
            'location': self.settings.default_location,
            'coverUrl': cover_url,
            'status': self.settings.default_status,
            'year': self.extract_year(str(book.get('pubdate', ''))),
            'description': self.clean_text(str(book.get('summary', '')))[:500],
            'languageCode': self.settings.default_language,
            'availability': self.settings.default_availability,
            'category': self.clean_text(category),
            'circulationPolicy': self.settings.default_circulation_policy,
            'totalCopies': self.settings.default_total_copies,
        }
