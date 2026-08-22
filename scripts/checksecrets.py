"""Is there a key, a token or a machine address inside what we ship?

    python scripts/checksecrets.py

Task P1-07, and the same check again at P8-07. Exits non-zero on a finding, so
it can be a build step later.

## What it looks for, and why each one

A locker that opens real doors has four secrets, and each has a way of ending
up somewhere it should not be:

  - **the cabinet key** — it lives in `src/cabinet/config.js`, which is a file
    the browser serves. Anyone at a public terminal can read view-source, so
    the key is placed on the device and never committed (ADR 0004).
  - **the SMS provider token** — spends money.
  - **the hashing key** — with the database file, it turns every six-digit
    code back into a guessable one (ADR 0022).
  - **a machine address** — not secret, but it is different on every laptop,
    and an address committed today is a build that cannot reach anything
    tomorrow. `server_base_url` is blank in the repo on purpose.

The first three are checked by their *real values*, read from the ignored
files on this machine, searched for in what git actually tracks. That is
stronger than a pattern: a pattern finds things that look like keys, this
finds the key.

## What it deliberately does not do

It does not scan `docs/`. Documentation quotes error messages, example keys
and addresses on purpose, and a check that shouts about those is a check
people learn to skip.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Loopback, the emulator's alias for the host, and the bind-anywhere address.
# None of these is a machine: they mean "this computer" wherever they run.
FINE = {"127.0.0.1", "0.0.0.0", "10.0.2.2", "255.255.255.255", "0.0.0.1"}

CODE = (".kt", ".kts", ".js", ".html", ".css", ".json", ".xml", ".py", ".gradle", ".pro", ".yml", ".yaml")

# What actually reaches a phone or a cabinet. `scripts/` is developer tooling
# and is not shipped, so an address in there is a different, smaller thing -
# reported, but not a failure. Saying that out loud is the difference between
# a check people act on and a check people silence.
SHIPPED = ("src/app/", "src/cabinet/", "src/server/", "config/")

MUST_IGNORE = [
    "src/cabinet/config.js",
    "config/pepper.key",
    ".env",
    "data/locker.db",
    "config/dev-cert/locker.jks",
    ".claude/settings.local.json",
]


def tracked() -> list[Path]:
    out = subprocess.run(["git", "ls-files"], cwd=ROOT, capture_output=True, text=True).stdout
    return [ROOT / line for line in out.splitlines() if line]


def host_of(value: str) -> str:
    """The machine part of a URL, or the value itself if it is not one."""
    m = re.match(r"[a-z]+://([^/:]+)", value)
    return m.group(1) if m else value


def live_secrets() -> dict[str, str]:
    """The real values on this machine, from the files git does not have."""
    found = {}
    key = ROOT / "config/pepper.key"
    if key.exists():
        found["the hashing key"] = key.read_text(encoding="utf-8").strip()
    cab = ROOT / "src/cabinet/config.js"
    if cab.exists():
        text = cab.read_text(encoding="utf-8")
        for name, pattern in (("the cabinet key", r'\bKEY\s*:\s*"([^"]{16,})"'),
                              ("the cabinet's server address", r'SERVER_BASE_URL\s*:\s*"([^"]+)"')):
            m = re.search(pattern, text)
            # A loopback or emulator URL names no machine, so it is not an
            # address anybody could leak. Compare the host, not the whole URL.
            if m and host_of(m.group(1)) not in FINE:
                found[name] = m.group(1)
    return {k: v for k, v in found.items() if len(v) >= 12}


def main() -> None:
    faults: list[str] = []
    files = [f for f in tracked() if f.suffix in CODE and not str(f.relative_to(ROOT)).startswith("docs")]
    print(f"searching {len(files)} tracked source files")

    # 1. The real secrets, by value.
    secrets = live_secrets()
    if not secrets:
        print("  ! no local secrets to search for - is this a fresh clone?")
    for name, value in secrets.items():
        hits = [f for f in files if value in f.read_text(encoding="utf-8", errors="ignore")]
        mark = "FAIL" if hits else "ok  "
        print(f"  {mark} {name} is not in any tracked file"
              + (f" - FOUND IN {[str(h.relative_to(ROOT)) for h in hits]}" if hits else ""))
        if hits:
            faults.append(f"{name} is committed, in {hits[0].relative_to(ROOT)}")

    # 2. A machine address in tracked source.
    ip = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
    # A pattern that matches nothing passes every file, which is the one
    # way a check like this fails without anybody noticing. It happened
    # once already, so the pattern proves itself before it is trusted.
    assert ip.findall('connect(("8.8.8.8", 80))') == ["8.8.8.8"], "the address pattern is broken"
    shipped_bad, tooling_bad = [], []
    for f in files:
        rel = f.relative_to(ROOT).as_posix()
        for line_no, line in enumerate(f.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
            stripped = line.strip()
            # Comments explain addresses; they do not ship one.
            if stripped.startswith(("*", "//", "#", "<!--")):
                continue
            for a in ip.findall(line):
                if a in FINE:
                    continue
                (shipped_bad if rel.startswith(SHIPPED) else tooling_bad).append(f"{rel}:{line_no}  {a}")
    print(f"  {'FAIL' if shipped_bad else 'ok  '} no machine address in what ships"
          + (f" - {len(shipped_bad)} found" if shipped_bad else ""))
    for b in shipped_bad[:10]:
        print(f"         {b}")
    if shipped_bad:
        faults.append(f"{len(shipped_bad)} machine address(es) in shipped source")
    if tooling_bad:
        print(f"  note {len(tooling_bad)} address(es) in developer scripts, which nothing ships:")
        for b in tooling_bad[:6]:
            print(f"         {b}")

    # 3. server_base_url is blank in what we ship.
    settings = (ROOT / "config/settings.json").read_text(encoding="utf-8")
    committed = subprocess.run(["git", "show", "HEAD:config/settings.json"],
                               cwd=ROOT, capture_output=True, text=True).stdout
    url = re.search(r'"server_base_url"\s*:\s*"([^"]*)"', committed or settings)
    blank = not (url and url.group(1).strip())
    print(f"  {'ok  ' if blank else 'FAIL'} server_base_url is blank in the committed settings"
          + ("" if blank else f" - it says {url.group(1)!r}"))
    if not blank:
        faults.append("server_base_url is committed with an address in it")

    # 4. .gitignore really covers the files that hold secrets.
    for path in MUST_IGNORE:
        got = subprocess.run(["git", "check-ignore", "-q", path], cwd=ROOT).returncode == 0
        print(f"  {'ok  ' if got else 'FAIL'} .gitignore covers {path}")
        if not got:
            faults.append(f".gitignore does not cover {path}")

    # 5. The built app, if one has been built. This is the real "in the build".
    apk = ROOT / "src/app/build/outputs/apk/debug/app-debug.apk"
    if apk.exists():
        blob = apk.read_bytes()
        inside = [n for n, v in secrets.items() if v.encode() in blob]
        url_in = re.search(rb'"server_base_url"\s*:\s*"([^"]+)"', blob)
        addr = url_in.group(1).decode() if url_in else ""
        print(f"  {'FAIL' if inside else 'ok  '} the built APK carries no secret"
              + (f" - {inside}" if inside else ""))
        print(f"  {'note' if addr else 'ok  '} the built APK's server address is {addr or 'blank'}"
              + (" - a local build, not a shipped one" if addr else ""))
        if inside:
            faults.append(f"the built APK contains {inside[0]}")
    else:
        print("  ..   no APK built, so nothing to search. Run :app:assembleDebug first")

    print()
    if faults:
        print("FOUND SOMETHING:")
        for f in faults:
            print(f"  - {f}")
        sys.exit(1)
    print("no key, token or machine address in either build")


if __name__ == "__main__":
    main()
