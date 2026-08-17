"""Point the app at a server a real phone can reach, then say what to run next.

    python scripts/testbuild.py            # this laptop, on the wifi it is on
    python scripts/testbuild.py --url https://xyz.trycloudflare.com

Writes `server_base_url` into `config/settings.json` and stops. It does not
build and it does not upload, because both are one command each and hiding
them behind a third makes them harder to think about, not easier.

## Why this exists

The address is baked into the build - `Settings.kt` reads it from the shipped
`settings.json` and never from the server, because the app has to know where
the server is before it can ask the server anything. So a build carries
exactly one address, and a build made for the emulator (`10.0.2.2`) reaches
nothing at all from a real phone. The failure is silent: the app opens, looks
correct, and cannot sign in.

`config/settings.json` is a file a person edits, and this is the one field in
it that changes every time the laptop joins a different wifi. Reading it off
`ipconfig` by hand is where the typos come from.

## The certificate, which is the real obstacle

A LAN address is served with the certificate in `config/dev-cert`, which
nothing signed. A debug build trusts certificates a person installed by hand
(`src/debug/res/xml/network_security_config.xml`), so **each phone has to
install `locker.crt` once**. That is fine for one or two phones on this team.

It is not fine for twelve students, and twelve is the number Play wants for
fourteen days (P8-12). For that, put a real certificate in front of the
server and pass its address with `--url`. A Cloudflare quick tunnel is one
way and needs no account, no card and no domain:

    cloudflared tunnel --url https://localhost:8443 --no-tls-verify

The phone then talks HTTPS to a certificate every phone already trusts, and
nobody installs anything. The address changes each time the tunnel restarts,
so the build has to be remade - which is what this script is for.
"""

from __future__ import annotations

import argparse
import json
import re
import socket
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETTINGS = ROOT / "config/settings.json"
PORT = 8443


def lan_address() -> str:
    """This machine's address on the wifi it is actually using.

    Opening a UDP socket to a public address picks the interface the routing
    table would really use, without sending anything. `gethostname()` was the
    obvious way and it is wrong on a laptop with a second adapter - it can
    answer with the address of a virtual switch nothing else can reach.
    """
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        return probe.getsockname()[0]
    finally:
        probe.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", help="a full base url, for a tunnel or a real host")
    args = parser.parse_args()

    url = args.url.rstrip("/") if args.url else f"https://{lan_address()}:{PORT}"
    if not url.startswith("https://"):
        raise SystemExit("https only - the app refuses anything else before it opens a socket")

    # Edited as text, not re-serialised. This file is written for a person to
    # read: it carries comments about every number in it, and json.dump would
    # throw all of that away to change one string.
    text = SETTINGS.read_text(encoding="utf-8")
    field = re.compile(r'("server_base_url"\s*:\s*)"[^"]*"')
    if not field.search(text):
        raise SystemExit(f"no server_base_url field in {SETTINGS}")
    SETTINGS.write_text(field.sub(lambda m: f'{m.group(1)}"{url}"', text), encoding="utf-8")

    # Proves the file still parses. A settings file that has stopped being
    # JSON fails inside the app, on a phone, as a blank screen.
    assert json.loads(SETTINGS.read_text(encoding="utf-8"))["server_base_url"] == url

    print(f"server_base_url = {url}\n")
    print("next, in order:")
    print("  ./gradlew :server:run                        the server, on this laptop")
    print("  ./gradlew :app:assembleDebug                 the build the testers get")
    print("  ./gradlew :app:appDistributionUploadDebug    send it to the team group")
    if "trycloudflare" not in url and args.url is None:
        print("\nthe phone must be on this same wifi, and must install")
        print(f"  {ROOT / 'config/dev-cert/locker.crt'}")
        print("once, under Settings > Security > Install a certificate > CA certificate.")
    print("\nnever commit config/settings.json with an address in it.")


if __name__ == "__main__":
    main()
