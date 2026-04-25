import importlib.util
from pathlib import Path


def load_retry_module():
    module_path = Path(__file__).resolve().parents[1] / 'scripts' / 'retry_uncovered_covers.py'
    spec = importlib.util.spec_from_file_location('retry_uncovered_covers', module_path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


retry = load_retry_module()


def test_normalize_title_variants_extracts_useful_fragments() -> None:
    variants = retry.normalize_title_variants('计算机科学丛书·Java语言程序设计进阶篇')

    assert 'Java语言程序设计进阶篇' in variants


def test_find_local_cover_matches_variant_with_same_author() -> None:
    title_author_index, title_index = retry.build_local_cover_index(
        [
            {
                'title': 'Computer Architecture',
                'author': 'John L. Hennessy / David A. Patterson',
                'coverUrl': '/book-covers/douban-1098.jpg',
            }
        ]
    )

    match = retry.find_local_cover(
        {
            'title': 'Computer Architecture, Fifth Edition',
            'author': 'John L. Hennessy / David A. Patterson',
        },
        title_author_index,
        title_index,
    )

    assert match == '/book-covers/douban-1098.jpg'


def test_parse_search_subject_items_reads_window_data() -> None:
    html = """
    <script>
    window.__DATA__ = {"count": 1, "items": [
      {"tpl_name": "search_more", "urls": []},
      {
        "tpl_name": "search_subject",
        "id": 1488876,
        "title": "深入浅出设计模式（影印版）",
        "abstract": "Eric Freeman / Elisabeth Freeman / 东南大学出版社 / 2005-11 / 98.00元",
        "abstract_2": "",
        "cover_url": "https://img3.doubanio.com/view/subject/m/public/s2414323.jpg"
      }
    ]};
    </script>
    """

    items = retry.parse_search_subject_items(html)

    assert items == [
        {
            'id': '1488876',
            'title': '深入浅出设计模式（影印版）',
            'author_name': 'Eric Freeman / Elisabeth Freeman / 东南大学出版社 / 2005-11 / 98.00元',
            'year': '2005',
            'pic': 'https://img3.doubanio.com/view/subject/m/public/s2414323.jpg',
        }
    ]


def test_choose_match_rejects_noise_when_search_results_are_generic() -> None:
    match = retry.choose_match(
        {
            'title': '111个失败的案例',
            'author': '(美)杰拉德·斯考尼沃夫 / 徐光兴',
            'year': '2007',
        },
        [
            {
                'id': '5975558',
                'title': '11.1',
                'author_name': '町田 ひらく / 一水社 / 2000-11 / JPY 860',
                'year': '2000',
                'pic': 'https://img1.doubanio.com/view/subject/m/public/s4633030.jpg',
            }
        ],
        min_score=35,
    )

    assert match is None


def test_choose_match_accepts_strong_search_result() -> None:
    match = retry.choose_match(
        {
            'title': 'Head First Design Patterns—深入淺出設計模式',
            'author': '未知作者',
            'year': '',
        },
        [
            {
                'id': '1400656',
                'title': 'Head First Design Patterns',
                'author_name': "Elisabeth Freeman / Eric Freeman / O'Reilly Media / 2004-11-1 / USD 49.99",
                'year': '2004',
                'pic': 'https://img1.doubanio.com/view/subject/m/public/s7019630.jpg',
            }
        ],
        min_score=35,
    )

    assert match is not None
    assert match['id'] == '1400656'
