"""Drive the whole locker through the real server and say what happened.

Not a unit test. This starts the server the way `./gradlew :server:run` does,
over real TLS on a real socket, and walks one parcel from a shipper standing at
a cabinet to a receiver opening a door with a phone. Then it tries to steal it
four ways.

    python scripts/checkserver.py

What it proves, in the order it proves it:

  1. A phone registers with a one-time code that really went through the SMS
     provider - the demo one, which prints instead of sending.
  2. A shipper finds that person by phone number and gets a MASKED name back.
  3. A drop opens a box and writes a parcel against that receiver.
  4. The phone sees the parcel.
  5. The phone scans the cabinet's QR and the right door is told to open.
  6. The ESP32 asks for work and is handed that door, exactly once.

  Then the four attacks from P5-10, all of which must be refused BY THE SERVER:

  7. An invented session code.
  8. A real code from the Library, used to collect a parcel at the Back Gate.
  9. Another person's parcel, with your own valid token.
 10. The same code and parcel collected twice.

  Then the two things that only bite a database that has lived a while:

 11. An open nobody ever confirmed gives the parcel back, instead of stranding
     its owner outside their own box forever.
 12. The schema migrates by number, and a second start on the same file does
     not try to add the column again.

TLS is verified against config/dev-cert/locker.crt - the checker does not
disable certificate checking, because a check that passes on a broken
certificate would hide the one thing the front-ends refuse to do without.
"""

from __future__ import annotations

import json
import os
import re
import socket
import sqlite3
import ssl
import subprocess
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = Path(__file__).resolve().parent.parent
CERT = ROOT / "config" / "dev-cert" / "locker.crt"
PORT = 8449
BASE = f"https://localhost:{PORT}"

BACK_GATE = "vgu-back-gate"
LIBRARY = "vgu-library"

ALICE = "0908619328"
BOB = "0912345678"

failures: list[str] = []


def say(mark: str, text: str) -> None:
    print(f"  {mark} {text}", flush=True)


def check(condition: bool, text: str) -> bool:
    say("OK  " if condition else "FAIL", text)
    if not condition:
        failures.append(text)
    return condition


# --- talking to it --------------------------------------------------------


def context() -> ssl.SSLContext:
    if not CERT.exists():
        sys.exit(f"no certificate at {CERT} - start the server once to make one")
    return ssl.create_default_context(cafile=str(CERT))


def call(method, path, body=None, token=None, key=None):
    """One request. Returns (status, parsed body)."""
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Accept", "application/json")
    if data:
        req.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    if key:
        req.add_header("X-Cabinet-Key", key)
    try:
        with urllib.request.urlopen(req, context=context(), timeout=20) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw or "{}")
        except json.JSONDecodeError:
            return e.code, {"raw": raw}


def refusal(answer) -> str:
    return (answer[1] or {}).get("code", f"HTTP {answer[0]}")


# --- running it -----------------------------------------------------------


def kill_whatever_holds_the_port() -> None:
    """Last resort: find the process on PORT and end it.

    Only ever aimed at our own port, and only after a terminate that did not
    take. On Windows a process started from a shell script survives its
    parent, which is how one of these was left running across three runs.
    """
    try:
        if os.name == "nt":
            out = subprocess.run(["netstat", "-ano"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if f":{PORT} " in line and "LISTENING" in line:
                    subprocess.run(["taskkill", "/F", "/PID", line.split()[-1]],
                                   capture_output=True)
        else:
            out = subprocess.run(["lsof", "-ti", f"tcp:{PORT}"],
                                 capture_output=True, text=True).stdout
            for pid in out.split():
                subprocess.run(["kill", "-9", pid], capture_output=True)
    except Exception:
        # Nothing here is worth failing a run over. The port check that
        # follows is what actually decides.
        pass


def free_port_or_die() -> None:
    """Refuse to run if something is already on the port.

    A leftover server answering these requests is the worst possible outcome:
    every check passes, against a build that no longer exists. It cost an hour
    once, and the whole session's evidence was wrong while it did.
    """
    with socket.socket() as probe:
        probe.settimeout(2)
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            sys.exit(
                f"something is already listening on {PORT}.\n"
                f"It is probably a server left over from an earlier run.\n"
                f"  netstat -ano | grep :{PORT}      then    taskkill /F /PID <pid>"
            )



def assert_the_build_is_not_stale(jars: Path) -> None:
    """Refuse to check a build that is older than the code it came from.

    This script runs `installDist` output rather than Gradle, for a good
    reason written below - but nothing made it notice when that output was
    old. On 2026-08-22 it ran a **five-day-old** server and printed
    `All checks passed`, which is the most expensive kind of green: a whole
    afternoon's work - a hashing key, two password endpoints, a schema
    column that decides whether a parcel is written off - none of it in the
    thing being checked, and the report saying it was fine.

    A stale jar even passes the schema check, because a server built at
    version 2 migrates a fresh database to version 2 and agrees with itself.

    Timestamps, not hashes. This has to be cheap enough that nobody is
    tempted to skip it.
    """
    newest_jar = max((j.stat().st_mtime for j in jars.glob("*.jar")), default=0.0)
    source = ROOT / "src" / "server" / "src" / "main"
    newer = [f for f in source.rglob("*.kt") if f.stat().st_mtime > newest_jar]
    if not newer:
        return
    print(f"the build is older than the code: {len(newer)} source file(s) have changed since")
    for f in sorted(newer)[:5]:
        print(f"  {f.relative_to(ROOT).as_posix()}")
    if len(newer) > 5:
        print(f"  ... and {len(newer) - 5} more")
    sys.exit("run: ./gradlew :server:installDist   then check again")


class Server:
    """The real server, started the way a person starts it."""

    def __init__(self) -> None:
        self.lines: list[str] = []
        self.proc: subprocess.Popen | None = None
        self.db = Path(tempfile.mkdtemp(prefix="checkserver-")) / "locker.db"

    def start(self, fresh: bool = True) -> dict[str, str]:
        jars = ROOT / "src" / "server" / "build" / "install" / "server" / "lib"
        if not jars.is_dir():
            sys.exit(f"nothing built at {jars} - run: ./gradlew :server:installDist")
        assert_the_build_is_not_stale(jars)

        # Java is started directly rather than through Gradle's launcher
        # script. The script is a shell wrapper, and terminating a wrapper on
        # Windows leaves the java process underneath still holding the port -
        # the next run then binds nothing, talks to the leftover server from
        # the run before, and reports on a build that is no longer there.
        free_port_or_die()

        env = {
            **os.environ,
            "PORT": str(PORT),
            "LOCKER_DB": str(self.db),
            "LOCKER_SEED": "1",
        }
        env.pop("SPEEDSMS_TOKEN", None)  # the demo provider, never a real send

        self.proc = subprocess.Popen(
            ["java", "-Dfile.encoding=UTF-8", "-cp", str(jars / "*"),
             "vn.edu.vgu.smartlocker.server.MainKt"],
            cwd=str(ROOT),
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        threading.Thread(target=self._drain, daemon=True).start()

        keys = self._wait_for_keys(fresh)
        say("OK  ", f"server up on {BASE}, {'fresh' if fresh else 'existing'} database")
        return keys

    def _drain(self) -> None:
        assert self.proc and self.proc.stdout
        for line in self.proc.stdout:
            self.lines.append(line.rstrip())

    def _wait_for_keys(self, fresh: bool = True) -> dict[str, str]:
        """Wait until it answers, reading the cabinet keys on the way.

        Keys are printed once, when a cabinet is created. A restart on a
        database that already has its cabinets prints none, so a second start
        waits only for the server to be up.
        """
        wanted = 2 if fresh else 0
        keys: dict[str, str] = {}
        deadline = time.time() + 90
        current = None
        while time.time() < deadline:
            for line in list(self.lines):
                if match := re.search(r"^\s+(vgu-[\w-]+)\s+\(", line):
                    current = match.group(1)
                if (match := re.search(r"CABINET KEY: (\S+)", line)) and current:
                    keys[current] = match.group(1)
            if len(keys) >= wanted and any("Application started" in ln for ln in self.lines):
                return keys
            if self.proc and self.proc.poll() is not None:
                print("\n".join(self.lines[-25:]))
                sys.exit("the server stopped before it was ready")
            time.sleep(0.4)
        print("\n".join(self.lines[-25:]))
        sys.exit("the server never became ready")

    def last_code(self, phone_e164: str) -> str | None:
        """The one-time code the demo provider printed, for this number."""
        for line in reversed(self.lines):
            if phone_e164 in line and (match := re.search(r"là (\d{4,8})", line)):
                return match.group(1)
        return None

    def stop(self) -> None:
        """Stop it, and then make sure it is actually stopped.

        Asking politely is not enough, and this is the second time that has
        bitten: a server left holding the port answers the next run's requests,
        every check passes, and the report is about a build that no longer
        exists. So the port is what gets checked, not the process handle.
        """
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                self.proc.kill()

        for _ in range(10):
            with socket.socket() as probe:
                probe.settimeout(1)
                if probe.connect_ex(("127.0.0.1", PORT)) != 0:
                    return
            kill_whatever_holds_the_port()
            time.sleep(0.5)
        print(f"  WARN something is still on {PORT} - the next run will refuse to start")


def register(server: Server, phone: str) -> str:
    """A whole registration, and the token it ends with."""
    status, _ = call("POST", "/auth/request-code", {"phone_number": phone})
    if status != 200:
        sys.exit(f"asking for a code for {phone} answered {status}")

    e164 = "+84" + phone.lstrip("0")
    code = None
    for _ in range(20):
        code = server.last_code(e164)
        if code:
            break
        time.sleep(0.2)
    if not code:
        sys.exit(f"the demo provider never printed a code for {e164}")

    status, body = call("POST", "/auth/verify-code", {"phone_number": phone, "code": code})
    if status != 200 or not body.get("token"):
        sys.exit(f"verifying {phone} answered {status} {body}")
    return body["token"]


# --- looking at the file itself -------------------------------------------
#
# Three checks below read the database directly. Everything else in this file
# goes through the wire on purpose, but a migration and a timeout are both
# facts about the FILE, and there is no endpoint that reports either.

# One entry per migration in `Schema.kt`, in order, named so a reader can see
# what version 6 actually means. The count is the check: a migration added
# without a line here, or a line here without a migration, fails.
#
# It went stale between 2026-08-17 and 2026-08-22 and nobody noticed, because
# the jars this script runs were five days old and a stale build lands on the
# version it was built with. See `assert_the_build_is_not_stale`.
MIGRATIONS_EXPECTED = [
    "the tables",
    "opening_since",
    "codes and tokens cleared for the pepper",
    "commands.purpose",
    "password_hash, tries and lockout",
    "google_sub",
]


def sql(db: Path, statement: str, *args):
    """One statement against the live database. The server is idle here."""
    with sqlite3.connect(str(db), timeout=5) as conn:
        return conn.execute(statement, args).fetchall()


def user_version(db: Path) -> int:
    return sql(db, "PRAGMA user_version")[0][0]


def parcel_state(db: Path) -> str:
    rows = sql(db, "SELECT state FROM parcels ORDER BY arrived_at DESC LIMIT 1")
    return rows[0][0] if rows else ""


def age_the_open(db: Path, seconds: int) -> None:
    """Make the open look older than it is, so the timeout is reachable."""
    sql(db, "UPDATE parcels SET opening_since = ? WHERE state = 'opening'",
        int(time.time() * 1000) - seconds * 1000)


def main() -> int:
    server = Server()
    try:
        keys = server.start()
        back_gate, library = keys[BACK_GATE], keys[LIBRARY]

        print("\nRegistering, dropping, collecting")

        alice = register(server, ALICE)
        bob = register(server, BOB)
        check(bool(alice and bob and alice != bob), "two phones registered, two different tokens")

        # A wrong code must not work, and must say nothing useful.
        answer = call("POST", "/auth/verify-code", {"phone_number": ALICE, "code": "000000"})
        check(refusal(answer) == "WRONG_CODE", f"a wrong one-time code is refused: {refusal(answer)}")

        # The shipper, at the back gate.
        status, found = call("GET", f"/cabinet/receiver?phone_number={ALICE}", key=back_gate)
        masked = found.get("masked_name", "")
        check(status == 200 and bool(found.get("ref")), "the shipper finds her by phone number")
        check(
            ALICE not in json.dumps(found) and "+84" not in json.dumps(found),
            f"the cabinet is told no phone number back (got {found!r})",
        )
        check("*" in masked, f"and the name it shows is masked: {masked!r}")

        status, dropped = call(
            "POST", "/cabinet/drop",
            {"receiver_ref": found["ref"], "size": "medium"}, key=back_gate,
        )
        box = dropped.get("box_number", "")
        check(status == 200 and box.isdigit() and len(box) == 2,
              f"a drop opened box {box!r} - two digits, leading zero kept")

        # Her phone.
        status, mine = call("GET", "/parcels", token=alice)
        check(status == 200 and len(mine.get("parcels", [])) == 1,
              f"her phone shows exactly one parcel: {mine.get('parcels')}")
        check(mine["parcels"][0]["box_number"] == box, "and it is the box the shipper filled")

        status, empty = call("GET", "/parcels", token=bob)
        check(status == 200 and empty.get("parcels") == [],
              "the other phone shows nothing - a parcel is not everybody's")

        # The scan.
        _, session = call("GET", "/cabinet/session", key=back_gate)
        code = session.get("session_code", "")
        check(len(code) > 20, "the cabinet screen was issued a session code")

        # The shape the app parses, checked against a code the server really
        # issued. This is the only place both ends run at once, so it is the
        # only place the two halves of `VGU1|cabinet|seconds|random` can be
        # compared for real rather than by grepping each other's source.
        fields = code.split("|")
        check(len(fields) == 4, f"it has the four fields SessionCode.kt reads: {len(fields)}")
        check(fields[0] == "VGU1", f"the format marker is VGU1: {fields[0]!r}")
        check(fields[1] == BACK_GATE, f"it names the cabinet that issued it: {fields[1]!r}")
        check(
            fields[2].isdigit() and abs(int(fields[2]) - time.time()) < 120,
            f"the clock reading is unix seconds, and now: {fields[2]!r}",
        )
        check(len(fields[3]) >= 32, f"and the random field is long enough to be random: {len(fields[3])}")

        status, opened = call("POST", "/parcels/collect", {"session_code": code}, token=alice)
        check(status == 200 and opened.get("box_number") == box,
              f"she scanned it and box {opened.get('box_number')} was told to open")

        # The hardware.
        _, work = call("GET", "/cabinet/commands", key=back_gate)
        jobs = work.get("commands", [])
        check(any(j["box"] == box and j["action"] == "open" for j in jobs),
              f"the ESP32 asked for work and was handed box {box}")

        _, again = call("GET", "/cabinet/commands", key=back_gate)
        check(again.get("commands") == [],
              "asking a second time is handed nothing - a command opens one door once")

        print("\nThe four attacks (P5-10). Every one must be refused here, not on the phone")

        # 1. An invented code.
        answer = call("POST", "/parcels/collect", {"session_code": "VGU1-made-up"}, token=alice)
        check(refusal(answer) == "SESSION_EXPIRED",
              f"a made-up session code is refused: {refusal(answer)}")

        # 2. A real code from the wrong cabinet.
        _, other = call("GET", "/cabinet/session", key=library)
        answer = call("POST", "/parcels/collect", {"session_code": other["session_code"]}, token=alice)
        check(refusal(answer) == "NO_PARCEL_HERE",
              f"a Library code does not open a Back Gate box: {refusal(answer)}")

        # 3. Somebody else's parcel, with a valid token of your own.
        _, fresh = call("GET", "/cabinet/session", key=back_gate)
        answer = call("POST", "/parcels/collect", {"session_code": fresh["session_code"]}, token=bob)
        check(refusal(answer) == "NO_PARCEL_HERE",
              f"her parcel does not open for his token: {refusal(answer)}")

        # 4. The same code twice.
        answer = call("POST", "/parcels/collect", {"session_code": code}, token=alice)
        check(refusal(answer) == "NO_PARCEL_HERE",
              f"the same code cannot open the same box twice: {refusal(answer)}")

        # And no token at all.
        answer = call("POST", "/parcels/collect", {"session_code": code})
        check(refusal(answer) == "TOKEN_EXPIRED", f"no token, no door: {refusal(answer)}")

        # A cabinet call with no key.
        answer = call("GET", "/cabinet/session")
        check(refusal(answer) == "CABINET_UNKNOWN", f"no cabinet key, no session: {refusal(answer)}")

        print("\nThe refusals must not tell them apart")
        check(
            refusal(call("POST", "/parcels/collect", {"session_code": other["session_code"]}, token=alice))
            == refusal(call("POST", "/parcels/collect", {"session_code": fresh["session_code"]}, token=bob)),
            "'nothing here for you' and 'not yours' read the same",
        )

        print("\nThe log is the evidence")
        status, history = call("GET", "/parcels/history", token=alice)
        actions = [e["action"] for e in history.get("events", [])]
        check("collect-opened" in actions, f"her open is written down: {actions}")

        # The app's ledger draws a box number, a place and a date. The place
        # comes from here, joined to the cabinet - the event row only stores an
        # id, and a ledger row with no place is a row nobody can act on.
        places = {e.get("cabinet_name", "") for e in history.get("events", [])}
        check(
            places and all(places),
            f"every history line says which cabinet, by name: {places}",
        )
        _, his = call("GET", "/parcels/history", token=bob)
        check(his.get("events") == [], "and it is not in his history")

        print("\nAn open nobody confirmed must not strand the parcel")

        # The door was told to open at line 342 and no `door-closed` ever came
        # - the shipper walked off, or the screen slept. Before `opening_since`
        # the parcel stayed `opening` for good, and step 2 of Collect only ever
        # matches `waiting`, so Alice was told NOTHING IS WAITING HERE about
        # her own parcel, in a box the server still held for her.
        check(parcel_state(server.db) == "opening",
              f"her parcel is mid-open, waiting for a door-closed that never comes: {parcel_state(server.db)}")

        _, s1 = call("GET", "/cabinet/session", key=back_gate)
        answer = call("POST", "/parcels/collect", {"session_code": s1["session_code"]}, token=alice)
        check(refusal(answer) == "NO_PARCEL_HERE",
              f"scanning again straight away still refuses - the open is believed for now: {refusal(answer)}")

        # Wind the clock on that one row rather than waiting two real minutes.
        # The recovery itself is not faked: the server does the deciding, on
        # its own clock, in the same query a real timeout would run.
        age_the_open(server.db, seconds=600)

        _, s2 = call("GET", "/cabinet/session", key=back_gate)
        status, back = call("POST", "/parcels/collect", {"session_code": s2["session_code"]}, token=alice)
        check(status == 200 and back.get("box_number") == box,
              f"after the timeout she gets her own parcel back, box {back.get('box_number')!r}")

        print("\nThe schema knows which version it is on")
        check(user_version(server.db) == len(MIGRATIONS_EXPECTED),
              f"the database is stamped version {user_version(server.db)}")

    finally:
        server.stop()

    # Starting again on the SAME file is the whole point of numbered steps.
    # `ALTER TABLE ... ADD COLUMN` run twice is an error, so a second start is
    # the only thing that proves a migration is skipped rather than re-run.
    print("\nAnd starting again on the same database changes nothing")
    second = Server()
    second.db = server.db
    try:
        second.start(fresh=False)
        status, still = call("GET", "/parcels", token=alice)
        check(status == 200, "the server starts a second time on a database it already migrated")
        check(user_version(server.db) == len(MIGRATIONS_EXPECTED),
              f"still version {user_version(server.db)} - the steps ran once, not twice")
    finally:
        second.stop()

    print()
    if failures:
        print(f"FAILED - {len(failures)} of them")
        for line in failures:
            print(f"  - {line}")
        return 1
    print("All checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
