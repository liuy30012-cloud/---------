from pathlib import Path
from unittest.mock import Mock

from book_import_tool.importer import BackendImporter
from book_import_tool.settings import load_settings


def test_login_uses_env_backed_credentials(tmp_path: Path) -> None:
    settings = load_settings(tmp_path)
    session = Mock()
    response = Mock()
    response.json.return_value = {'success': True, 'data': {'token': 'jwt-token'}}
    session.post.return_value = response

    importer = BackendImporter(settings, session=session)

    assert importer.login() is True
    assert importer.token == 'jwt-token'
    assert session.post.call_args.kwargs['json'] == {
        'studentId': settings.admin_username,
        'password': settings.admin_password,
    }


def test_import_file_returns_success_counts(tmp_path: Path) -> None:
    settings = load_settings(tmp_path)
    excel_file = tmp_path / 'books_001.xlsx'
    excel_file.write_bytes(b'test')

    session = Mock()
    response = Mock()
    response.json.return_value = {
        'success': True,
        'data': {'successCount': 3, 'failedCount': 1},
    }
    session.post.return_value = response

    importer = BackendImporter(settings, session=session)
    importer.token = 'jwt-token'

    result = importer.import_file(excel_file)

    assert result == {
        'file': 'books_001.xlsx',
        'success': True,
        'successCount': 3,
        'failureCount': 1,
    }
