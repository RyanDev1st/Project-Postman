"""Is there a key, a token or a machine address inside what we ship?

    python scripts/checksecrets.py

Task P1-07, and the same check again at P8-07. Exits non-zero on a finding, so
it can be a build step later.

## What it looks for, and why each one

A locker that opens real doors has five secrets, and each has a way of ending
up somewhere it should not be:

  - **the cabinet key** — it lives in `src/cabinet/config.js`, which is a file
    the browser serves. Anyone at a public terminal can read view-source, so
    the key is placed on the device and never committed (ADR 0004).
  - **the SMS provider token** — spends money.
  - **the hashing key** — with the database file, it turns every six-digit
    code back into a guessable one (ADR 0022).
  - **the Google and reCAPTCHA credentials** — they live in `/.env` since
    ADR 0026 made a Google account the way in. Only the ones that are really
    credentials are searched for. A project id, an app id, an OAuth client id
    and a reCAPTCHA **site** key sit in the same file but are public by
    design: they ship inside the APK and appear in URLs, and treating them as
    secrets would make this check shout about committed documentation.
  - **a machine address** — not secret, but it is different on every laptop,
    and an address committed today is a build that cannot reach anything
    tomorrow. `server_base_url` is blank in the repo on purpose.

The first four are checked by their *real values*, read from the ignored
files on this machine, searched for in what git actually tracks. That is
stronger than a pattern: a pattern finds things that look like keys, this
finds the key.

It is also narrower, and the narrowness shows on a machine that has none of
those files - a fresh clone, or CI - where the value search has nothing to
search for and passes everything. So there is a second pass over the same
files for the *shapes* a key comes in. The two cover each other: the values
find a secret that looks like a word, the shapes find one nobody here has a
copy of.

## What it deliberately does not do

It does not scan `docs/`. Documentation quotes error messages, example keys
and addresses on purpose, and a check that shouts about those is a check
people learn to skip.
"""

from __future__ import annotations

import io
import re
import subprocess
import sys
import tempfile
import zipfile
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

# Shapes that are a key whatever file they are in. Deliberately short: every
# entry here is a form that has exactly one meaning, so a hit is never a
# discussion. `google-services.json` is not tracked and is not searched.
SHAPES = [
    ("a Google API key", re.compile(r"AIza[0-9A-Za-z_\-]{35}")),
    ("a Google OAuth client id", re.compile(r"\d{6,}-[0-9a-z]{20,}\.apps\.googleusercontent\.com")),
    ("a reCAPTCHA key", re.compile(r"6L[0-9A-Za-z_\-]{38}")),
    ("a private key block", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("a service-account file", re.compile(r'"type"\s*:\s*"service_account"')),
    ("a signed token", re.compile(r"eyJ[0-9A-Za-z_\-]{8,}\.eyJ[0-9A-Za-z_\-]{8,}")),
    ("an AWS access key", re.compile(r"AKIA[0-9A-Z]{16}")),
]

# Which `/.env` names are credentials rather than public identifiers. A list
# and not a guess from the word "key": `RECAPTCHA_SITE_KEY` is published in
# the page it protects and `RECAPTCHA_API_KEY` is not, and they differ by one
# word.
CREDENTIAL = re.compile(r"(?i)(_API_KEY|_SECRET|_TOKEN|_PASSWORD|_PRIVATE_KEY|CREDENTIALS)$")

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
    env = ROOT / ".env"
    if env.exists():
        for line in env.read_text(encoding="utf-8").splitlines():
            name, sep, value = line.strip().partition("=")
            if sep and CREDENTIAL.search(name.strip()):
                found[name.strip()] = value.strip().strip('"').strip("'")
    return {k: v for k, v in found.items() if len(v) >= 12}



def search_apk(apk: Path, secrets: dict[str, str]) -> tuple[list[str], str]:
    """Which secrets are inside a built APK, and what address it carries.

    **An APK is a zip, so its bytes are not its contents.** Reading the file
    and searching the bytes is what this used to do, and it could not fail:
    `res/raw/settings.json` is deflated, so the address really baked into the
    build was invisible, and so would a key have been. It reported a blank
    address for a build that carried `https://10.0.2.2:8443`. Every entry is
    decompressed here and searched.

    The address is read from `res/raw/settings.json`, which is the copy the
    app itself reads - `Settings.readShipped`. Falling back to any JSON that
    happens to match would report a number from some library's asset.
    """
    found: list[str] = []
    addr = ""
    with zipfile.ZipFile(apk) as z:
        for entry in z.infolist():
            if entry.is_dir():
                continue
            try:
                blob = z.read(entry)
            except (RuntimeError, zipfile.BadZipFile):
                # Encrypted or damaged. Say so rather than skipping quietly:
                # an entry nobody can read is an entry nobody has checked.
                print(f"         ! could not read {entry.filename} - not searched")
                continue
            for name, value in secrets.items():
                if value.encode() in blob and name not in found:
                    found.append(name)
            if entry.filename == "res/raw/settings.json":
                m = re.search(rb'"server_base_url"\s*:\s*"([^"]*)"', blob)
                if m:
                    addr = m.group(1).decode()
    return found, addr


def assert_apk_scanner_works() -> None:
    """Prove the scanner can see through compression before trusting it.

    The address pattern above proves itself the same way, for the same
    reason: this check has now silently passed everything twice - once on a
    corrupted regex, once on unread compressed entries - and both times the
    output was a screen of `ok`.
    """
    needle = "a-secret-that-must-be-found"
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("classes.dex", needle * 40)
        z.writestr("res/raw/settings.json", '{"server_base_url": "https://example.test:1"}')
    buf.seek(0)
    raw = buf.getvalue()
    assert needle.encode() not in raw, "the self-test needle was not compressed, so it proves nothing"
    tmp = Path(tempfile.gettempdir()) / "checksecrets-selftest.apk"
    tmp.write_bytes(raw)
    try:
        found, addr = search_apk(tmp, {"the needle": needle})
        assert found == ["the needle"], "the APK scanner cannot see inside a compressed entry"
        assert addr == "https://example.test:1", "the APK scanner cannot read the shipped settings"
    finally:
        tmp.unlink(missing_ok=True)


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

    # 1b. The shapes, for a key this machine has no copy of.
    shaped = []
    for f in files:
        rel = f.relative_to(ROOT).as_posix()
        for line_no, line in enumerate(f.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
            for what, shape in SHAPES:
                hit = shape.search(line)
                if hit:
                    # Four characters is enough to recognise and not enough to
                    # use. This script's output goes into commit messages.
                    shaped.append(f"{rel}:{line_no}  {what} - {hit.group(0)[:4]}…")
    print(f"  {'FAIL' if shaped else 'ok  '} nothing shaped like a key in tracked source"
          + (f" - {len(shaped)} found" if shaped else ""))
    for b in shaped[:10]:
        print(f"         {b}")
    if shaped:
        faults.append(f"{len(shaped)} thing(s) shaped like a key are committed")

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

    # 4b. Ignored today is not the same as never committed. Git keeps what it
    # was once given, so a key added and then ignored is still a leaked key.
    for path in (".env", "config/pepper.key", "src/cabinet/config.js"):
        ever = subprocess.run(["git", "log", "--all", "--oneline", "--", path],
                              cwd=ROOT, capture_output=True, text=True).stdout.strip()
        print(f"  {'FAIL' if ever else 'ok  '} no commit ever touched {path}"
              + (f" - {len(ever.splitlines())} did" if ever else ""))
        if ever:
            faults.append(f"{path} is in the history of {len(ever.splitlines())} commit(s)")

    # 5. The built app, if one has been built. This is the real "in the build".
    apk = ROOT / "src/app/build/outputs/apk/debug/app-debug.apk"
    if apk.exists():
        assert_apk_scanner_works()
        inside, addr = search_apk(apk, secrets)
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
