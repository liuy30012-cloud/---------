from pathlib import Path

from book_import_tool.settings import load_settings
from book_import_tool.transform import BookTransformer


def test_convert_book_normalizes_valid_payload(tmp_path: Path) -> None:
    transformer = BookTransformer(load_settings(tmp_path))

    payload = {
        'title': '  <b>Clean Code</b>  ',
        'author': ['Robert C. Martin'],
        'isbn13': '978-0132350884',
        'pubdate': '2008-08',
        'summary': ' Practical\n software craftsmanship ',
        'tags': [{'name': 'Software'}],
        'images': {'large': 'https://example.test/cover.jpg'},
    }

    converted = transformer.convert_book(payload)

    assert converted is not None
    assert converted['title'] == 'Clean Code'
    assert converted['author'] == 'Robert C. Martin'
    assert converted['isbn'] == '9780132350884'
    assert converted['year'] == '2008'
    assert converted['category'] == 'Software'
    assert converted['coverUrl'] == 'https://example.test/cover.jpg'


def test_convert_book_rejects_invalid_isbn(tmp_path: Path) -> None:
    transformer = BookTransformer(load_settings(tmp_path))

    payload = {
        'title': 'Bad Book',
        'author': ['Nobody'],
        'isbn13': 'invalid-isbn',
    }

    assert transformer.convert_book(payload) is None
