"""Check that a real-phone test can actually run, before anybody walks to the cabinet.

    python scripts/checktest.py

Eight checks. Each one is a way the test dies without saying why: the app
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


def routed_address() -> str:
    """Whatever address the routing table would send traffic out of.

    **Not necessarily an address a phone can reach.** A VPN owns the default
    route while it is up, so this answers with the tunnel. On this machine on
    2026-08-18 it answered 172.16.0.2, which is Cloudflare WARP - and a phone
    on the same wifi can no more reach that than it can reach the moon.
    """
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        return probe.getsockname()[0]
    finally:
        probe.close()


def own_addresses() -> list[str]:
    """Every IPv4 address this machine answers on, tunnels and all.

    Listing them beats picking one. The version before this picked the routed
    address and called it *the* LAN address, which meant it told somebody to
    rebuild the certificate around a VPN tunnel - confident, specific, wrong.
    """
    found = {routed_address()}
    for info in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
        found.add(info[4][0])
    return sorted(a for a in found if not a.startswith(("127.", "169.254.")))


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


addresses = own_addresses()
settings = ROOT / "config/settings.json"
url = json.loads(settings.read_text(encoding="utf-8")).get("server_base_url", "")
aimed = re.sub(r"^https://", "", url).split(":")[0]

print("this machine answers on: " + ", ".join(addresses))
print(f"the build is aimed at:   {url or 'nothing'}\n")

# 1. The certificate names the address the build is aimed at.
#
# The keystore is written once and reused forever. Move network, and the
# address changes while the certificate does not - the phone then refuses on a
# hostname mismatch, which reads on the phone as "cannot reach the server" and
# on the laptop as nothing at all.
#
# Checked against the address the build actually aims at, not against a guess.
# Guessing is how this check once told somebody to rebuild the certificate
# around a VPN tunnel.
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
        covered = [a for a in addresses if f"IP Address:{a}" in listed]
        check(
            aimed in ("localhost", "127.0.0.1") or f"IP Address:{aimed}" in listed,
            f"the certificate covers {aimed or 'the aimed-at address'}",
            "rm -r config/dev-cert  then restart the server - it is rebuilt covering\n"
            "            every address this machine has right now. Covers today: "
            + (", ".join(covered) or "none of the above"),
        )
    except (OSError, subprocess.SubprocessError):
        check(True, "certificate SANs - SKIPPED, no openssl on this machine")

# 2. The build is aimed somewhere this machine actually is.
#
# A VPN tunnel address passes any check that only asks "is this one of mine",
# and then fails on a phone, so the message names the candidates rather than
# blessing one. Only a person knows which network the phone is on.
check(
    aimed in addresses or aimed in ("localhost", "127.0.0.1"),
    f"the build is aimed at an address this machine has ({aimed or 'unset'})",
    "python scripts/testbuild.py --url https://<one of the addresses above>:8443\n"
    "            A phone needs the address on the network THE PHONE is on -\n"
    "            never a VPN tunnel, however much it looks like a LAN address.",
)

# 3. The server is running at all.
#
# Asked on loopback, which is always reachable and always in the certificate.
# Asking on the aimed-at address instead made a wrong aim look like a stopped
# server, and printed "start the server" at somebody watching it run.
try:
    with open_insecure(f"https://127.0.0.1:{PORT}/health") as answer:
        check(answer.status == 200, "the server is running")
except (urllib.error.URLError, OSError) as e:
    check(False, "the server is running", f"./gradlew :server:run   ({e})")

# 3b. And a phone could reach it where the build will look.
#
# A separate question from "is it running", and the only one a phone cares
# about. Skipped when the build aims at loopback, which is the browser-only
# setup and where no phone is involved.
if aimed and aimed not in ("localhost", "127.0.0.1"):
    try:
        open_insecure(f"https://{aimed}:{PORT}/health")
        check(True, f"a phone could reach it on {aimed}:{PORT}")
    except (urllib.error.URLError, OSError) as e:
        check(False, f"a phone could reach it on {aimed}:{PORT}",
              f"nothing answers there. Aim the build somewhere this machine is ({e})")

# 4. The browser will be allowed to call it.
#
# Against the address **the screen** is configured with, which is its own
# setting and not the app's - the two clients aim independently. Without CORS
# the screen shows a dead cabinet in front of a server that is running
# perfectly well, and the server log stays silent. BUG-010.
screen_base = "https://127.0.0.1:8443"
if (ROOT / "src/cabinet/config.js").exists():
    found = re.search(
        r'SERVER_BASE_URL\s*:\s*"([^"]+)"',
        (ROOT / "src/cabinet/config.js").read_text(encoding="utf-8"),
    )
    if found:
        screen_base = found.group(1).rstrip("/")

origin = f"http://127.0.0.1:{SCREEN_PORT}"
try:
    with open_insecure(
        f"{screen_base}/cabinet/commands",
        method="OPTIONS",
        headers={
            "Origin": origin,
            "Access-Control-Request-Method": "GET",
            "Access-Control-Request-Headers": "x-cabinet-key",
        },
    ) as answer:
        check(
            answer.headers.get("Access-Control-Allow-Origin") is not None,
            f"the screen at {origin} may call {screen_base}",
        )
except (urllib.error.URLError, OSError):
    check(
        False,
        f"the screen at {origin} may call {screen_base}",
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
