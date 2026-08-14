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

# Waivers. One line per file, and every line names the ADR that decided it.
#
# A waiver is not a way to quiet the checker. It is the record of a decision
# that was argued somewhere a person can read, and the ADR reference is the
# part that matters: a waiver nobody can trace back is indistinguishable from
# a rule quietly dropped. If the ADR is ever reversed, the entry goes with it
# and this file starts failing again, which is the point.
#
# Keyed by path so a waiver covers one file and not a folder. The listed
# reasons are the only ones forgiven in that file - everything else in it is
# still checked.
WAIVERS: dict[str, tuple[str, list[str]]] = {
    # The route to the gate is fetched from a public router. It must NOT go
    # through Http.kt: that door attaches the receiver's bearer token to
    # every request, and a third-party router has no business holding a token
    # that opens lockers. Its own connection, HTTPS only.
    "src/app/src/main/kotlin/vn/edu/vgu/smartlocker/parcels/map/Walk.kt": (
        "ADR 0016 - the router gets its own connection, never the app's door",
        ["HttpURLConnection", "URL(", "openConnection", "a hard-coded address"],
    ),
    # The map tile style URL. OpenFreeMap needs no key and no account, so
    # this address is not a secret - it is the public endpoint of a public
    # service, and MapLibre has to be handed it directly.
    "src/app/src/main/kotlin/vn/edu/vgu/smartlocker/parcels/map/LiveMap.kt": (
        "ADR 0014 - OpenFreeMap tiles, no key, no account, no card",
        ["a hard-coded address"],
    ),
}


def waived(rel: str, what: str) -> bool:
    """Whether this exact finding, in this exact file, was decided in an ADR."""
    entry = WAIVERS.get(rel)
    return bool(entry) and what in entry[1]


# P1-07. Things that must never be built into a shipped front-end.
SECRETS = [
    # Reserved names are exempt, and only reserved names. RFC 2606 and 6761
    # set aside .invalid, .example, .test and .localhost precisely so that a
    # documented example cannot become a live endpoint by accident. A
    # template file is not exempt as a file - a real key pasted into
    # config.example.js is still caught by the rules below it.
    (r"https?://(?!schemas\.android\.com|www\.w3\.org)"
     r"(?![a-z0-9.-]*\.(?:invalid|example|test|localhost)\b)"
     r"[a-z0-9.-]+\.[a-z]{2,}",
     "a hard-coded address"),
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
                if waived(rel, name):
                    continue
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
                if waived(rel, what):
                    continue
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

    # Always shown, pass or fail. A waiver that only the source reveals is a
    # rule quietly dropped; printed every run, it stays something a person
    # has to keep agreeing with.
    if WAIVERS:
        print("\nwaived, each by a decision written down:")
        for rel, (why, what) in sorted(WAIVERS.items()):
            print(f"  {rel}")
            print(f"      {', '.join(what)}")
            print(f"      {why}")

    if failed:
        print("\nnot clean")
        return 1

    print(f"one door, no secrets  ({len(app)} app files, {len(cabinet)} cabinet files)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
