from __future__ import annotations

import argparse
import html
import os
import re
import subprocess
import time
import urllib.parse
import urllib.request
from pathlib import Path


BASE = "https://registry.cuhk.edu.cn"
LISTING_URL = BASE + "/taxonomy/term/152"
STOP_PAGE_ID = "62"

TARGET_PROGRAMS = [
    ("经管学院", "大数据管理与应用", "363"),
    ("经管学院", "经济学", "65"),
    ("经管学院", "金融学", "64"),
    ("经管学院", "国际商务", "66"),
    ("经管学院", "市场营销", "67"),
    ("经管学院", "会计学", "63"),
    ("理工学院", "化学", "235"),
    ("理工学院", "电子与计算机工程", "275"),
    ("理工学院", "电子信息工程", "61"),
    ("理工学院", "材料科学与工程", "289"),
    ("理工学院", "数学与应用数学", "59"),
    ("理工学院", "新能源科学与工程", "60"),
    ("理工学院", "物理学", "270"),
    ("人文社科学院", "应用心理学", "51"),
    ("人文社科学院", "英语", "23"),
    ("人文社科学院", "国际组织与全球治理", "348"),
    ("人文社科学院", "翻译", "52"),
    ("人文社科学院", "城市管理", "344"),
    ("数据科学学院", "计算机科学与技术", "57"),
    ("数据科学学院", "数据科学与大数据技术", "56"),
    ("数据科学学院", "统计学", "58"),
    ("医学院", "生物信息学", "55"),
    ("医学院", "生物科学", "233"),
    ("医学院", "生物医学工程", "53"),
    ("医学院", "药学", "234"),
    ("人工智能学院", "人工智能", "364"),
    ("数据科学学院、经管学院及理工学院（联合创办）", "金融工程", "62"),
]


def fetch(url: str) -> bytes:
    try:
        return subprocess.check_output(
            [
                "curl.exe",
                "-L",
                "--silent",
                "--show-error",
                "--fail",
                "--connect-timeout",
                "20",
                "--max-time",
                "90",
                "-A",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                url,
            ],
            stderr=subprocess.STDOUT,
        )
    except (FileNotFoundError, subprocess.CalledProcessError):
        pass
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept": "text/html,application/pdf,*/*",
        },
    )
    with urllib.request.urlopen(req, timeout=45) as resp:
        return resp.read()


def decode_html(data: bytes) -> str:
    for enc in ("utf-8", "gb18030"):
        try:
            return data.decode(enc)
        except UnicodeDecodeError:
            pass
    return data.decode("utf-8", errors="replace")


def strip_tags(text: str) -> str:
    text = re.sub(r"<script[\s\S]*?</script>", " ", text, flags=re.I)
    text = re.sub(r"<style[\s\S]*?</style>", " ", text, flags=re.I)
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", html.unescape(text)).strip()


def clean_name(text: str, fallback: str = "file") -> str:
    text = html.unescape(strip_tags(text) if "<" in text else text)
    text = re.sub(r"[\\/:*?\"<>|]", "_", text)
    text = re.sub(r"\s+", " ", text).strip(" .")
    return text or fallback


def listing_links(page_html: str) -> list[tuple[str, str]]:
    start = page_html.find('class="list-1-0')
    if start < 0:
        start = page_html.find("list-body outside-md-t")
    end = page_html.find("wgt-pagination", start)
    if end < 0:
        end = page_html.find("pagination", start)
    segment = page_html[start:end if end > start else None]
    out: list[tuple[str, str]] = []
    item_re = re.compile(
        r'<div\s+class=["\']title["\']>\s*<a\b[^>]*href=["\']([^"\']*/page/(\d+)[^"\']*)["\'][^>]*>([\s\S]*?)</a>',
        re.I,
    )
    for match in item_re.finditer(segment):
        href = urllib.parse.urljoin(BASE, html.unescape(match.group(1)))
        title = clean_name(match.group(3), fallback=f"page_{match.group(2)}")
        if title and not any(x[0] == href for x in out):
            out.append((href, title))
    return out


def collect_program_pages(stop_page_id: str | None = STOP_PAGE_ID) -> list[tuple[str, str]]:
    ordered: list[tuple[str, str]] = []
    seen = set()
    for page in range(0, 12):
        url = LISTING_URL if page == 0 else f"{LISTING_URL}?page={page}"
        text = decode_html(fetch(url))
        links = listing_links(text)
        if not links and page > 0:
            break
        for href, title in links:
            page_id = re.search(r"/page/(\d+)", href)
            if not page_id or href in seen:
                continue
            seen.add(href)
            ordered.append((href, title))
            if stop_page_id and page_id.group(1) == stop_page_id:
                return ordered
        time.sleep(0.2)
    if stop_page_id:
        raise RuntimeError(f"Could not find the stop page /page/{stop_page_id} in listing pages.")
    return ordered


def target_program_pages() -> list[tuple[str, str, str]]:
    return [
        (BASE + f"/page/{page_id}", f"{school}__{program}", f"{school} / {program}")
        for school, program, page_id in TARGET_PROGRAMS
    ]


def covers_2023_or_later(text: str) -> bool:
    compact = re.sub(r"\s+", "", text)
    applicable = re.search(r"适用于(.+?)入学", compact)
    scope = applicable.group(1) if applicable else compact
    if re.search(r"20(23|24|25|26|27|28|29)[\-_至到—–-]*(?:\d{2}|20\d{2})", scope):
        return True
    if re.search(r"20(23|24|25|26|27|28|29).*?(以后|及以后|andthereafter|andonwards|andonward|orlater)", scope, re.I):
        return True
    ranges = re.findall(r"20(\d{2})\s*[-_]\s*(\d{2}|20\d{2})", scope)
    for start, end in ranges:
        start_year = int("20" + start)
        if start_year >= 2023:
            return True
        if int(end[-2:]) >= 24 and start_year >= 2021:
            return True
    return False


def pdf_links(page_html: str) -> list[tuple[str, str]]:
    links: list[tuple[str, str]] = []
    pattern = re.compile(r'<a\b[^>]*href=["\']([^"\']+\.pdf(?:\?[^"\']*)?)["\'][^>]*>([\s\S]*?)</a>', re.I)
    matches = list(pattern.finditer(page_html))
    for idx, match in enumerate(matches):
        href = urllib.parse.urljoin(BASE, html.unescape(match.group(1)))
        next_start = match.end()
        next_end = matches[idx + 1].start() if idx + 1 < len(matches) else min(len(page_html), match.end() + 900)
        nearby = strip_tags(match.group(2) + " " + page_html[next_start:next_end])
        url_name = urllib.parse.unquote(Path(urllib.parse.urlparse(href).path).name)
        label = clean_name(nearby, fallback=url_name)
        # Match admission-year scope, not upload/revision dates embedded in URL folders.
        if covers_2023_or_later(label + " " + re.sub(r"_\d{6,8}|circular[^.]*|3rd\(\d{4}\)|1st\d{4}|2nd\d{4}", "", url_name, flags=re.I)):
            links.append((href, label if label.lower().endswith(".pdf") else url_name))
    return links


def download_file(url: str, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    data = fetch(url)
    tmp = path.with_suffix(path.suffix + ".download")
    tmp.write_bytes(data)
    tmp.replace(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    output = Path(args.output)
    pages = target_program_pages()
    manifest: list[str] = []
    expected_files: list[str] = []
    total = 0

    for page_index, page_info in enumerate(pages, 1):
        page_url, folder_title, display_title = page_info
        page_id = re.search(r"/page/(\d+)", page_url)
        program_dir = output / f"{page_index:02d}_{clean_name(folder_title)}"
        text = decode_html(fetch(page_url))
        pdfs = pdf_links(text)
        manifest.append(f"{page_index:02d}. {display_title} ({page_url})")
        if not pdfs:
            manifest.append("    - no 2023+ PDF found")
        for href, label in pdfs:
            filename = clean_name(label, fallback=Path(urllib.parse.urlparse(href).path).name)
            if not filename.lower().endswith(".pdf"):
                filename += ".pdf"
            dest = program_dir / filename
            expected_files.append(str(dest.relative_to(output)))
            manifest.append(f"    - {filename} <- {href}")
            total += 1
            if not args.dry_run:
                if dest.exists() and dest.stat().st_size > 0:
                    continue
                download_file(href, dest)
                time.sleep(0.15)
        if False and page_id and page_id.group(1) == STOP_PAGE_ID:
            break
        time.sleep(0.2)

    output.mkdir(parents=True, exist_ok=True)
    (output / "_download_manifest.txt").write_text("\n".join(manifest) + "\n", encoding="utf-8")
    (output / "_expected_files.txt").write_text("\n".join(expected_files) + "\n", encoding="utf-8")
    print(f"Program pages: {len(pages)}")
    print(f"2023+ PDFs matched: {total}")
    print(f"Output: {output}")


if __name__ == "__main__":
    main()
