"""The whole parcel journey, with no phone and no cabinet in the room.

    python scripts/checkloop.py

A receiver registers, a shipper drops a parcel to them, the cabinet's
electronics report the door, the receiver scans the screen, and the box opens.
Nine steps, each one checked against the server's own answer.

## What this replaces, and what it does not

It replaces **the device**, not the software. Every call below is the real
endpoint over real TLS with a real cabinet key, and the server cannot tell
this from a phone and a Pi. What it cannot replace is a camera reading a QR
off a screen, a thumb on a touchscreen, and a door.

So this proves the rules are right. It does not prove the app is usable, and
it never proves metal moved - ADR 0008.

## Reading its own one-time code

Step 2 recovers the code by brute force from the hash in the database: it is
six digits and the hash is a plain unsalted SHA-256, so a million guesses
settle it in about a second.

That is a legitimate thing for a test on its own database to do, and it is
also a finding - anybody who can read `locker.db` can recover every live code
the same way. See docs/findings/2026-08-18-audit.md. The alternative was to
scrape the server's console, which would tie this test to how the server
happened to be started.
"""

from __future__ import annotations

import base64
import hashlib
import json
import random
import re
import sqlite3
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CERT = ROOT / "config/dev-cert/locker.crt"
DB = ROOT / "data/locker.db"
BASE = "https://127.0.0.1:8443"

steps: list[tuple[bool, str, str]] = []


def step(ok: bool, name: str, saw: str = "") -> bool:
    steps.append((ok, name, saw))
    print(f"{'OK  ' if ok else 'FAIL'} {name}" + (f"  -  {saw}" if saw else ""))
    if not ok:
        report()
        sys.exit(1)
    return ok


def report() -> None:
    bad = sum(1 for ok, _, _ in steps if not ok)
    print()
    print("the whole loop works" if not bad else f"stopped at step {len(steps)}")


def call(path: str, body: dict | None = None, token: str = "", key: str = "") -> tuple[int, dict]:
    """One request. Returns the status and the parsed body, refusals included."""
    ctx = ssl.create_default_context(cafile=str(CERT))
    ctx.check_hostname = False  # the certificate names the IP, not `localhost`
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(BASE + path, data=data)
    request.add_header("Accept", "application/json")
    if data is not None:
        request.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    if key:
        request.add_header("X-Cabinet-Key", key)
    try:
        with urllib.request.urlopen(request, context=ctx, timeout=15) as answer:
            raw = answer.read()
            return answer.status, (json.loads(raw) if raw else {})
    except urllib.error.HTTPError as e:
        raw = e.read()
        return e.code, (json.loads(raw) if raw else {})


def cabinet_key() -> str:
    found = re.search(r'\bKEY\s*:\s*"([^"]+)"', (ROOT / "src/cabinet/config.js").read_text(encoding="utf-8"))
    if not found:
        sys.exit("no cabinet key in src/cabinet/config.js")
    return found.group(1)


def cabinet_id() -> str:
    found = re.search(r'\bCABINET_ID\s*:\s*"([^"]+)"', (ROOT / "src/cabinet/config.js").read_text(encoding="utf-8"))
    return found.group(1) if found else "vgu-back-gate"


def recover_code(phone: str) -> str:
    """Six digits, from the hash the server wrote down. See the module docstring."""
    db = sqlite3.connect(f"file:{DB}?mode=ro", uri=True)
    try:
        row = db.execute("SELECT code_hash FROM otp WHERE phone = ?", (phone,)).fetchone()
    finally:
        db.close()
    if not row:
        sys.exit(f"no code was issued for {phone} - did request-code succeed?")
    wanted = row[0]
    for n in range(1_000_000):
        guess = f"{n:06d}"
        digest = base64.urlsafe_b64encode(hashlib.sha256(guess.encode()).digest()).decode().rstrip("=")
        if digest == wanted.rstrip("="):
            return guess
    sys.exit("could not recover the code - the hash is not a bare SHA-256 of six digits")


def parcel_state(parcel_id: str) -> str:
    db = sqlite3.connect(f"file:{DB}?mode=ro", uri=True)
    try:
        row = db.execute("SELECT state FROM parcels WHERE id = ?", (parcel_id,)).fetchone()
    finally:
        db.close()
    return row[0] if row else "gone"


def box_state(box: str) -> str:
    db = sqlite3.connect(f"file:{DB}?mode=ro", uri=True)
    try:
        row = db.execute(
            "SELECT state FROM boxes WHERE cabinet_id = ? AND number = ?", (cabinet_id(), box)
        ).fetchone()
    finally:
        db.close()
    return row[0] if row else "gone"


def work_the_cabinet(key: str, expect_action: str) -> str:
    """Poll like the Pi does, fire the relay, say it happened. Returns the box."""
    for _ in range(20):
        _, answer = call("/cabinet/commands", key=key)
        for command in answer.get("commands", []):
            box = command["box"]
            call("/cabinet/command-done", {"id": command["id"], "result": "ok"}, key=key)
            return box
        time.sleep(0.5)
    sys.exit(f"the server never asked for a door ({expect_action})")


def main() -> None:
    key = cabinet_key()
    # A number nobody has used, so this run cannot pass on somebody else's
    # account. 9-digit local part beginning 3/5/7/8/9, per Phone.normalise.
    phone = "+849" + "".join(random.choice("0123456789") for _ in range(8))
    name = "Tran Thi Test"

    print(f"receiver {phone}   cabinet {cabinet_id()}\n")

    # 1. Ask for a code.
    status, _ = call("/auth/request-code", {"phone_number": phone})
    step(status == 200, "1. a one-time code was asked for", f"HTTP {status}")

    # 2. Read it back, the way only a test on its own database can.
    code = recover_code(phone)
    step(len(code) == 6, "2. the code was recovered from its hash", code)

    # 3. Register, with a name. Nothing else in the system ever collects one.
    status, session = call("/auth/verify-code", {"phone_number": phone, "code": code, "full_name": name})
    token = session.get("token", "")
    step(bool(token), "3. the code was accepted and a token issued", f"HTTP {status}")

    # 3b. A wrong code must not work. Checked on a second number so the good
    # account above is not spent proving it.
    other = "+849" + "".join(random.choice("0123456789") for _ in range(8))
    call("/auth/request-code", {"phone_number": other})
    status, _ = call("/auth/verify-code", {"phone_number": other, "code": "000000"})
    real = recover_code(other)
    step(status != 200 or real == "000000", "3b. a wrong code is refused", f"HTTP {status}")

    # 3c. Five wrong tries must not buy five more.
    #
    # `verify` used to delete the row when the tries ran out, and that row is
    # where the one-a-minute cooldown is counted from - so burning the five
    # let a new code be asked for immediately, with five fresh tries and
    # nothing in the way. A six-digit code is a million guesses; at this
    # server's measured rate that was minutes. BUG-013.
    burn = "+849" + "".join(random.choice("0123456789") for _ in range(8))
    call("/auth/request-code", {"phone_number": burn})
    for n in range(5):
        call("/auth/verify-code", {"phone_number": burn, "code": f"{n:06d}"})
    status, _ = call("/auth/request-code", {"phone_number": burn})
    step(status == 429, "3c. burning the five tries does not buy five more", f"HTTP {status}")

    # 4. The shipper finds the receiver. The name must come back masked.
    status, who = call(f"/cabinet/receiver?phone_number={urllib.parse.quote(phone)}", key=key)
    masked = who.get("masked_name", "")
    step(status == 200 and masked not in ("", "***"), "4. the shipper sees a masked name", masked)
    step(
        name not in masked and phone[-6:] not in masked,
        "4b. the full name and the number are not on the public screen",
        masked,
    )

    # 5. Drop a parcel. The server picks the box; the cabinet is told.
    status, dropped = call("/cabinet/drop", {"receiver_ref": who["ref"], "size": "medium"}, key=key)
    box = dropped.get("box_number", "")
    step(status == 200 and box != "", "5. the server opened a box for the drop", f"box {box}")

    # 6. The electronics collect the command and report the relay.
    relay_box = work_the_cabinet(key, "drop")
    step(relay_box == box, "6. the cabinet was asked for that same box", f"box {relay_box}")
    call("/cabinet/door-closed", {"box_number": box, "purpose": "drop"}, key=key)

    # 7. The receiver opens the app. This is where a notice would be, if one
    #    were built - it is not, so the app asks.
    status, mine = call("/parcels", token=token)
    parcels = mine.get("parcels", [])
    step(
        status == 200 and len(parcels) == 1 and parcels[0]["box_number"] == box,
        "7. the parcel is listed for its owner, in the right box",
        f"{len(parcels)} parcel(s)",
    )
    parcel_id = parcels[0]["id"]
    step(parcel_state(parcel_id) == "waiting", "7b. the parcel is waiting", parcel_state(parcel_id))
    step(box_state(box) == "taken", "7c. the box is taken", box_state(box))

    # 8. The receiver scans the screen. The QR says which cabinet and when,
    #    and nothing else - identity comes from the token.
    _, live = call("/cabinet/session", key=key)
    session_code = live.get("session_code") or live.get("code", "")
    step(bool(session_code), "8. the screen minted a session code", session_code[:24] + "…")

    status, opened = call("/parcels/collect", {"session_code": session_code}, token=token)
    step(
        status == 200 and opened.get("box_number") == box,
        "8b. the scan opened the right box",
        f"HTTP {status} box {opened.get('box_number')}",
    )

    # 8c. The same code in somebody else's hands must open nothing. This is the
    #     photograph-of-the-screen case, and it is the one that matters most.
    status, _ = call("/parcels/collect", {"session_code": session_code})
    step(status != 200, "8c. the same code with no token opens nothing", f"HTTP {status}")

    # 9. The door shuts and the parcel is written off.
    relay_box = work_the_cabinet(key, "collect")
    step(relay_box == box, "9. the cabinet was asked to open it again", f"box {relay_box}")
    call("/cabinet/door-closed", {"box_number": box, "purpose": "collect"}, key=key)

    step(parcel_state(parcel_id) == "collected", "9b. the parcel is collected", parcel_state(parcel_id))
    step(box_state(box) == "free", "9c. the box is free again", box_state(box))

    status, mine = call("/parcels", token=token)
    step(len(mine.get("parcels", [])) == 0, "9d. nothing is left waiting", f"{len(mine.get('parcels', []))}")

    report()


if __name__ == "__main__":
    main()
