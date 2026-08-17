"""Check that a real-phone test can actually run, before anybody walks to the cabinet.

    python scripts/checktest.py

Seven checks. Each one is a way the test dies without saying why: the app
opens, looks correct, and reaches nothing. Every failure prints the command
that fixes it.

This checks the *setup*. It does not test the app - only a phone does that.
See `docs/reference/real-world-test.md` for the run itself.
"""

from __future__ import annotations

import json
import re
import socket
import ssl
import subprocess
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PORT = 8443
SCREEN_PORT = 8137

results: list[tuple[bool, str, str]] = []


def check(ok: bool, name: str, fix: str = "") -> bool:
    results.append((ok, name, fix))
    return ok


def lan_address() -> str:
    """The address on the wifi actually in use, not whatever DNS thinks."""
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        return probe.getsockname()[0]
    finally:
        probe.close()


def open_insecure(url: str, method: str = "GET", headers: dict | None = None):
    """The dev certificate is self-signed, so verification is off here on purpose.

    This script is checking that a server answers, not that it is trustworthy.
    The phone does verify, which is what check 1 exists to protect.
    """
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    request = urllib.request.Request(url, method=method, headers=headers or {})
    return urllib.request.urlopen(request, timeout=5, context=ctx)


lan = lan_address()

# 1. The certificate names this address.
#
# The keystore is written once and then reused forever. Join a different wifi
# and the address changes, the certificate does not, and the phone refuses the
# connection on a hostname mismatch - which reads on the phone as "cannot
# reach the server" and reads on the laptop as nothing at all.
cert = ROOT / "config/dev-cert/locker.crt"
if not cert.exists():
    check(False, "certificate exists", "start the server once: ./gradlew :server:run")
else:
    try:
        text = subprocess.run(
            ["openssl", "x509", "-in", str(cert), "-noout", "-text"],
            capture_output=True, text=True, timeout=10,
        ).stdout
        names = re.search(r"Subject Alternative Name:\s*\n\s*(.+)", text)
        listed = names.group(1) if names else ""
        check(
            f"IP Address:{lan}" in listed,
            f"certificate covers {lan}",
            f"rm -r config/dev-cert  then  ./gradlew :server:run   (it is rebuilt with {lan} in it)",
        )
    except (OSError, subprocess.SubprocessError):
        check(True, "certificate SANs - SKIPPED, no openssl on this machine")

# 2. The build points at that address.
settings = ROOT / "config/settings.json"
url = json.loads(settings.read_text(encoding="utf-8")).get("server_base_url", "")
check(
    url == f"https://{lan}:{PORT}",
    f"settings.json points at https://{lan}:{PORT}",
    f"python scripts/testbuild.py      (it says {url or 'nothing'})",
)

# 3. The server answers.
try:
    with open_insecure(f"https://{lan}:{PORT}/health") as answer:
        check(answer.status == 200, f"server answers on {lan}:{PORT}")
except (urllib.error.URLError, OSError) as e:
    check(False, f"server answers on {lan}:{PORT}", f"./gradlew :server:run   ({e})")

# 4. The browser will be allowed to call it.
#
# Without this the cabinet screen shows a dead cabinet in front of a server
# that is running perfectly well, and the server log stays silent. BUG-010.
origin = f"http://127.0.0.1:{SCREEN_PORT}"
try:
    with open_insecure(
        f"https://{lan}:{PORT}/cabinet/commands",
        method="OPTIONS",
        headers={
            "Origin": origin,
            "Access-Control-Request-Method": "GET",
            "Access-Control-Request-Headers": "x-cabinet-key",
        },
    ) as answer:
        check(
            answer.headers.get("Access-Control-Allow-Origin") is not None,
            f"the screen at {origin} is allowed to call the server",
        )
except (urllib.error.URLError, OSError):
    check(
        False,
        f"the screen at {origin} is allowed to call the server",
        f'start the server with:  LOCKER_CABINET_ORIGINS="127.0.0.1:{SCREEN_PORT},localhost:{SCREEN_PORT}" ./gradlew :server:run',
    )

# 5. The cabinet screen knows which cabinet it is, and has its key.
config = ROOT / "src/cabinet/config.js"
if not config.exists():
    check(False, "src/cabinet/config.js exists", "cp src/cabinet/config.example.js src/cabinet/config.js")
else:
    body = config.read_text(encoding="utf-8")
    has = lambda k: bool(re.search(r'\b' + k + r'\s*:\s*"[^"]+"', body))
    check(has("CABINET_ID") and has("KEY"), "the screen has a cabinet id and a key",
          "./gradlew :server:run --args=\"cabinet add vgu-back-gate 'VGU Back Gate' 25\"  then paste the key in")

# 6. The screen is being served.
try:
    urllib.request.urlopen(f"http://127.0.0.1:{SCREEN_PORT}/index.html", timeout=3)
    check(True, f"cabinet screen is served on {SCREEN_PORT}")
except (urllib.error.URLError, OSError):
    check(False, f"cabinet screen is served on {SCREEN_PORT}",
          f"python -m http.server {SCREEN_PORT} --directory src/cabinet")

# 7. A build can be sent to a phone.
try:
    who = subprocess.run(["firebase", "login:list"], capture_output=True, text=True,
                         timeout=30, shell=True).stdout
    check("Logged in as" in who, "firebase can upload the build", "firebase login")
except (OSError, subprocess.SubprocessError):
    check(False, "firebase can upload the build", "npm install -g firebase-tools  then  firebase login")


print()
for ok, name, fix in results:
    print(f"{'OK  ' if ok else 'FAIL'} {name}")
    if not ok and fix:
        print(f"       fix: {fix}")

bad = sum(1 for ok, _, _ in results if not ok)
print()
print("ready for a phone" if not bad else f"{bad} thing(s) to fix first")
raise SystemExit(1 if bad else 0)
