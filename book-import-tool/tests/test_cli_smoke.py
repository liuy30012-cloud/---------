from book_import_tool.cli import run_pipeline


def test_run_pipeline_invokes_each_stage_once() -> None:
    events: list[str] = []

    class FakeCrawler:
        def crawl_books(self, total_books: int) -> None:
            events.append(f'crawl:{total_books}')

    class FakeProcessor:
        def process_all_batches(self) -> None:
            events.append('process')

    class FakeImporter:
        def import_all_files(self) -> None:
            events.append('import')

    run_pipeline(FakeCrawler, FakeProcessor, FakeImporter)

    assert events[0].startswith('crawl:')
    assert events[1:] == ['process', 'import']
