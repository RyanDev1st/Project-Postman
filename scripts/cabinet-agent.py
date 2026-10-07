"""Stand in for the cabinet's ESP32: poll the server, open a box, report back.

    python -u scripts/cabinet-agent.py
    python -u scripts/cabinet-agent.py --key <cabinet key>

With no `--key` it reads the one in `src/cabinet/config.js`, which is the same
cabinet this laptop's screen is already running as. That file is git-ignored
and is where the key belongs; typing it on a command line puts it in the shell
history of whoever ran it.

Run it on the laptop that is pretending to be the cabinet. It is a TEST
STAND-IN, not shipped code - ADR 0008's rule about the Blender cabinet applies
here too: **what this prints is not evidence a door moved.** It is evidence the
server issued a command and something collected it. The real evidence for a
real cabinet is a real door.

## What it does, and why that is all it does

The ESP32's whole job, per architecture.md section 3, is three calls:

  22. `GET  /cabinet/commands`      anything for me?
  23. `POST /cabinet/command-done`  I did it
  12. `POST /cabinet/door-closed`   the door is shut again

There is no fourth. It never decides anything - which box, whose parcel, and
whether a code is good are all the server's, and a cabinet that decided any of
them would be a second place the rules live.

## Why it stops and waits for a person

There is no door sensor - ADR 0006. So nothing on the cabinet can *know* a
door was shut. A stand-in that fired `door-closed` on a timer would be
simulating a sensor this cabinet does not have, and every test run after that
would be quietly testing a machine we do not own.

So it asks. The person at the keyboard is standing in for the person at the
cabinet, which is exactly who reports this today.
"""

from __future__ import annotations

import argparse
import json
import re
import ssl
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parent.parent
CERT = ROOT / "config/dev-cert/locker.crt"

# About once a second, which is what the ESP32 is specified to do. Slower and
# a person is standing at a shut door wondering; faster and a cabinet on a
# tired campus wifi is just making noise.
POLL_SECONDS = 1.0


def call(base: str, key: str, path: str, body: dict | None = None) -> dict:
    """One request, with the cabinet key on it.

    The certificate is checked against the file the server exported. Turning
    verification off here would make this stand-in prove less than the phone
    does, and the phone is the thing we are trying to test.
    """
    local = urlsplit(base).hostname in ("127.0.0.1", "localhost")
    context = ssl.create_default_context(cafile=str(CERT)) if local else ssl.create_default_context()
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(base + path, data=data)
    request.add_header("X-Cabinet-Key", key)
    request.add_header("Accept", "application/json")
    if data is not None:
        request.add_header("Content-Type", "application/json; charset=utf-8")
    with urllib.request.urlopen(request, context=context, timeout=10) as answer:
        raw = answer.read()
    return json.loads(raw) if raw else {}


def serve(base: str, key: str) -> None:
    print(f"cabinet agent -> {base}")
    print(f"certificate    {CERT if urlsplit(base).hostname in ('127.0.0.1', 'localhost') else 'system trust store'}")
    print("waiting for the server to ask for a door. ctrl-c to stop.\n")

    while True:
        try:
            answer = call(base, key, "/cabinet/commands")
        except urllib.error.HTTPError as e:
            sys.exit(f"server refused the key: HTTP {e.code} {e.reason}")
        except OSError as e:
            # A cabinet on campus wifi loses the server all the time. It keeps
            # asking rather than dying, because the alternative is a locker
            # that needs a person to restart it after every network blip.
            print(f"  (no server: {e}) retrying")
            time.sleep(POLL_SECONDS * 3)
            continue

        for command in answer.get("commands", []):
            handle(base, key, command)

        time.sleep(POLL_SECONDS)


def handle(base: str, key: str, command: dict) -> None:
    """One command: work the relay, say it worked, then wait for the door."""
    box = command.get("box_number") or command.get("box", "??")
    print(f"\n  [ {command.get('action')} box {box}")
    print(f"  |  RELAY {box} ON  -  latch released  -  door {box} is open")

    # Reported straight away and separately from the door closing. They are
    # different facts: the latch fired, and later, the door is shut. A cabinet
    # that reported them together would be reporting one it cannot see.
    call(base, key, "/cabinet/command-done", {"id": command.get("id"), "result": "ok"})
    print("  |  told the server: ok")

    purpose = command.get("purpose")
    if purpose in ("drop", "collect"):
        input(f"  |  shut door {box} after {purpose}, then press Enter: ")
    else:
        purpose = ask(box)
    call(base, key, "/cabinet/door-closed", {"box_number": box, "purpose": purpose})
    print(f"  `- told the server: door {box} shut after a {purpose}\n")


def ask(box: str) -> str:
    """Which kind of open was that, when the command does not say.

    It says now - BUG-009 put `purpose` on the command, because an ESP32 is
    handed a box number and has no honest way to work this out, and reporting
    the wrong one either books a collection that never happened or loses one
    that did.

    This is kept for a cabinet still running the older firmware, and for a
    command queued before that change. Asking a person is the only honest
    answer left at that point.
    """
    while True:
        answer = input(f"  |  shut door {box}, then type c (collected) or d (dropped): ")
        if answer.strip().lower().startswith("c"):
            return "collect"
        if answer.strip().lower().startswith("d"):
            return "drop"


def key_from_screen() -> str | None:
    """The key the cabinet screen on this laptop is already using.

    Same cabinet, same key, and it is one file rather than one more thing to
    keep in step. Returns None if there is no config yet, which is a clearer
    thing to report than a crash.
    """
    config = ROOT / "src/cabinet/config.js"
    if not config.exists():
        return None
    found = re.search(r'\bKEY\s*:\s*"([^"]+)"', config.read_text(encoding="utf-8"))
    return found.group(1) if found else None


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", default="https://127.0.0.1:8443")
    parser.add_argument("--key", help="the cabinet key. Default: the one in src/cabinet/config.js")
    args = parser.parse_args()

    if urlsplit(args.server).hostname in ("127.0.0.1", "localhost") and not CERT.exists():
        sys.exit(f"no certificate at {CERT} - start the server once to make one")

    key = args.key or key_from_screen()
    if not key:
        sys.exit(
            "no key. Either pass --key, or put one in src/cabinet/config.js:\n"
            '  ./gradlew :server:run --args="cabinet add vgu-back-gate \'VGU Back Gate\' 25"',
        )

    try:
        serve(args.server.rstrip("/"), key)
    except KeyboardInterrupt:
        print("\nstopped.")


if __name__ == "__main__":
    main()
