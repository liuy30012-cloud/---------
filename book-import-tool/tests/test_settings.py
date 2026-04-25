from pathlib import Path

from book_import_tool.settings import load_settings


def test_load_settings_reads_environment(monkeypatch, tmp_path: Path) -> None:
    monkeypatch.setenv('BACKEND_API_BASE', 'http://example.test/api')
    monkeypatch.setenv('ADMIN_USERNAME', 'tester')
    monkeypatch.setenv('TOTAL_BOOKS', '42')

    settings = load_settings(tmp_path)

    assert settings.base_dir == tmp_path
    assert settings.backend_api_base == 'http://example.test/api'
    assert settings.admin_username == 'tester'
    assert settings.total_books == 42
    assert settings.raw_data_dir == tmp_path / 'data' / 'raw'
