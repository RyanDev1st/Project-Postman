"""Prove that every network call goes through one file.

Rule 1 in docs/reference/architecture.md, and the Verify line of task P1-04:

    a search of the app source finds no HTTP call outside that one file

This is that search, written down so it can be re-run rather than remembered.
It also carries the P1-07 checks - no key, token or address built into either
front-end - because they read the same trees and nobody runs two scripts.

    python scripts/checknet.py

Exit code 0 means clean. Anything else means read the output.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# The one door in each front-end. Everything else must go through these.
APP_DOOR = ROOT / "src/app/src/main/kotlin/vn/edu/vgu/smartlocker/net/Http.kt"
CABINET_DOOR = ROOT / "src/cabinet/net.js"

APP_SRC = ROOT / "src/app/src/main/kotlin"
CABINET_SRC = ROOT / "src/cabinet"

# Ways to open a connection. If a new one appears, add it here in the same
# change - a rule nothing checks is a rule that has already been broken.
APP_NETWORK = [
    (r"\bHttpURLConnection\b", "HttpURLConnection"),
    (r"\bHttpsURLConnection\b", "HttpsURLConnection"),
    (r"\bURL\s*\(", "URL("),
    (r"\bOkHttpClient\b", "OkHttp"),
    (r"\bRetrofit\b", "Retrofit"),
    (r"\bSocket\s*\(", "Socket("),
    (r"\bopenConnection\b", "openConnection"),
]

CABINET_NETWORK = [
    (r"\bfetch\s*\(", "fetch("),
    (r"\bXMLHttpRequest\b", "XMLHttpRequest"),
    (r"\bnavigator\.sendBeacon\b", "sendBeacon"),
    (r"\bnew\s+WebSocket\b", "WebSocket"),
    (r"\bnew\s+EventSource\b", "EventSource"),
]

# P1-07. Things that must never be built into a shipped front-end.
SECRETS = [
    (r"https?://(?!schemas\.android\.com|www\.w3\.org)[a-z0-9.-]+\.[a-z]{2,}", "a hard-coded address"),
    (r"\bBearer\s+[A-Za-z0-9._~+/-]{16,}", "a bearer token"),
    # `\\?[\"']` because a secret inside a Kotlin or JS string literal reaches
    # us with its quotes escaped, and the first version of this pattern walked
    # straight past exactly that case.
    (r"\b(?:api[_-]?key|secret|password|passwd|token)\s*[:=]\s*\\?[\"'][^\"'\\]{8,}",
     "a key or password"),
    (r"\bAIza[0-9A-Za-z_-]{20,}", "a Google API key"),
    # A JWT by its own shape. `val TOKEN: String = "eyJ..."` puts a type
    # between the name and the `=`, so the name-based pattern above walks past
    # it. What a token looks like is harder to disguise than what it is called.
    (r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}", "a JWT"),
    (r"\b-----BEGIN [A-Z ]*PRIVATE KEY-----", "a private key"),
]

# Plain HTTP anywhere is a bug on its own - api-contract.md makes HTTPS a
# fixed rule, not a setting. XML namespaces are URIs, never fetched.
PLAIN_HTTP = re.compile(r"http://(?!schemas\.android\.com|www\.w3\.org|localhost|127\.0\.0\.1)")

SKIP_DIRS = {"build", ".gradle", "node_modules", "__pycache__"}


def source_files(root: Path, suffixes: tuple[str, ...]) -> list[Path]:
    if not root.exists():
        return []
    return sorted(
        p for p in root.rglob("*")
        if p.suffix in suffixes
        and p.is_file()
        and not any(part in SKIP_DIRS for part in p.parts)
    )


def strip_noise(text: str) -> str:
    """Blank out comments and doc blocks.

    A comment saying "no OkHttp here" is not an OkHttp call, and a checker
    that cannot tell the difference is a checker people learn to ignore.
    """
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"(?m)^\s*//.*$", "", text)
    text = re.sub(r"(?m)^\s*\*.*$", "", text)
    return text


def scan(files: list[Path], patterns, door: Path, label: str) -> list[str]:
    """Report every network call outside `door`."""
    problems = []
    for path in files:
        if path.resolve() == door.resolve():
            continue
        body = strip_noise(path.read_text(encoding="utf-8", errors="replace"))
        for pattern, name in patterns:
            for match in re.finditer(pattern, body):
                line = body[: match.start()].count("\n") + 1
                rel = path.relative_to(ROOT).as_posix()
                problems.append(
                    f"  {rel}:{line}  {name}\n"
                    f"      {label} calls go through {door.relative_to(ROOT).as_posix()}"
                )
    return problems


def scan_secrets(files: list[Path]) -> list[str]:
    problems = []
    for path in files:
        raw = path.read_text(encoding="utf-8", errors="replace")
        body = strip_noise(raw)
        for pattern, what in SECRETS:
            for match in re.finditer(pattern, body, re.I):
                line = body[: match.start()].count("\n") + 1
                rel = path.relative_to(ROOT).as_posix()
                problems.append(f"  {rel}:{line}  {what}: {match.group(0)[:60]}")
        for match in PLAIN_HTTP.finditer(body):
            line = body[: match.start()].count("\n") + 1
            rel = path.relative_to(ROOT).as_posix()
            problems.append(f"  {rel}:{line}  plain HTTP. HTTPS is fixed, not a setting")
    return problems


def check_gitignore() -> list[str]:
    """P1-07: the config files that hold secrets stay out of git."""
    ignore = (ROOT / ".gitignore")
    if not ignore.exists():
        return ["  .gitignore is missing"]
    body = ignore.read_text(encoding="utf-8")
    missing = [p for p in (".env", "local.properties", "google-services.json")
               if p not in body]
    return [f"  .gitignore does not cover {p}" for p in missing]


def check_settings_shipped() -> list[str]:
    """The settings file the app ships must be readable and hold no address."""
    path = ROOT / "config/settings.json"
    if not path.exists():
        return ["  config/settings.json is missing"]
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return [f"  config/settings.json will not parse: {e}"]
    url = data.get("server_base_url", "")
    if url and not url.startswith("https://"):
        return [f"  server_base_url is not https: {url}"]
    return []


def main() -> int:
    app = source_files(APP_SRC, (".kt",))
    cabinet = source_files(CABINET_SRC, (".js",))

    sections = [
        ("network calls outside the app's one door",
         scan(app, APP_NETWORK, APP_DOOR, "App")),
        ("network calls outside the cabinet's one door",
         scan(cabinet, CABINET_NETWORK, CABINET_DOOR, "Cabinet")),
        ("secrets built into a front-end",
         scan_secrets(app + cabinet)),
        ("config files not ignored by git", check_gitignore()),
        ("the shipped settings file", check_settings_shipped()),
    ]

    failed = False
    for title, problems in sections:
        if problems:
            failed = True
            print(f"\n{title}:")
            print("\n".join(problems))

    if failed:
        print("\nnot clean")
        return 1

    print(f"one door, no secrets  ({len(app)} app files, {len(cabinet)} cabinet files)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
