"""How much of a campus this server can hold, measured rather than guessed.

    python scripts/stress.py               50, 200 and 500 at once
    python scripts/stress.py --at 500      just the one level
    python scripts/stress.py --seconds 20  longer run, steadier numbers

## What it models

The peak this product actually has is **students opening the app**, not
parcels being dropped. A drop happens when a courier walks up; an app open
happens when 500 people finish a lecture at the same time. So the load is
read-heavy, and the writes are the cabinets reporting in.

    80%  GET /parcels          a student opening the app
    15%  GET /cabinet/commands a cabinet asking for work, once a second each
     5%  GET /cabinet/receiver a shipper looking somebody up

Every call is the real endpoint over real TLS with a real token or cabinet
key. Nothing is stubbed, and no endpoint is called that a client would not.

## What the number means

`500 concurrent` here means 500 requests in flight at once, which is a
heavier thing than 500 students using the app - a student opening the app
sends one request and then reads the screen for a while. If this holds 500 in
flight, it holds a few thousand students.

Latency is reported at p50, p95 and p99 because an average hides the
stalls, and a stall is what a person actually feels.
"""

from __future__ import annotations

import argparse
import atexit
import base64
import hashlib
import http.client
import json
import random
import re
import sqlite3
import ssl
import sys
import threading
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor
from typing import Callable
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CERT = ROOT / "config/dev-cert/locker.crt"
DB = ROOT / "data/locker.db"
BASE = "https://127.0.0.1:8443"
ACCOUNTS = 20

# One context for every thread. Building one per request is a measurable cost
# and it is the harness's cost, not the server's - it would show up as
# latency the server never spent.
CTX = ssl.create_default_context(cafile=str(CERT))
CTX.check_hostname = False

# One connection per thread, held open.
#
# **This is the difference between measuring the server and measuring TLS.**
# The first version of this script opened a fresh connection per request, and
# every endpoint answered at about the same 1,250 req/s - including /health,
# which touches no database. That was not the server's ceiling, it was the
# cost of 1,250 TLS handshakes a second on the same laptop. Reusing the
# connection, as OkHttp on Android and fetch in a browser both do, moved the
# same endpoint to 7,300 req/s.
_local = threading.local()


def _conn() -> http.client.HTTPSConnection:
    live = getattr(_local, "conn", None)
    if live is None:
        live = http.client.HTTPSConnection("127.0.0.1", 8443, context=CTX, timeout=30)
        _local.conn = live
    return live


def call(path: str, body: dict | None = None, token: str = "", key: str = "") -> tuple[int, dict, float]:
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = "application/json; charset=utf-8"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if key:
        headers["X-Cabinet-Key"] = key

    began = time.perf_counter()
    try:
        live = _conn()
        live.request("POST" if data is not None else "GET", path, body=data, headers=headers)
        answer = live.getresponse()
        raw = answer.read()
        return answer.status, (json.loads(raw) if raw else {}), time.perf_counter() - began
    except Exception:
        # A dead connection is thrown away rather than reused - a half-read
        # socket poisons every later request on it and would be counted as
        # server latency it never spent.
        try:
            if getattr(_local, "conn", None):
                _local.conn.close()
        except Exception:
            pass
        _local.conn = None
        return 0, {}, time.perf_counter() - began


def cabinet_key() -> str:
    found = re.search(r'\bKEY\s*:\s*"([^"]+)"', (ROOT / "src/cabinet/config.js").read_text(encoding="utf-8"))
    if not found:
        sys.exit("no cabinet key in src/cabinet/config.js")
    return found.group(1)


def rainbow() -> dict[str, str]:
    """Every six-digit code by its hash, built once.

    The server stores a bare unsalted SHA-256, so this is a million entries and
    about a second. Twenty accounts would otherwise be twenty brute forces.
    That it is this cheap is a finding in its own right - see the audit.
    """
    table = {}
    for n in range(1_000_000):
        guess = f"{n:06d}"
        digest = base64.urlsafe_b64encode(hashlib.sha256(guess.encode()).digest()).decode().rstrip("=")
        table[digest] = guess
    return table


def make_accounts(n: int) -> list[str]:
    """Real accounts with real tokens, made the way the app makes them."""
    print(f"building {n} accounts…", end=" ", flush=True)
    table = rainbow()
    tokens = []
    for _ in range(n):
        phone = "+849" + "".join(random.choice("0123456789") for _ in range(8))
        status, _, _ = call("/auth/request-code", {"phone_number": phone})
        if status != 200:
            continue
        db = sqlite3.connect(f"file:{DB}?mode=ro", uri=True)
        row = db.execute("SELECT code_hash FROM otp WHERE phone = ?", (phone,)).fetchone()
        db.close()
        code = table.get(row[0].rstrip("=")) if row else None
        if not code:
            continue
        status, session, _ = call(
            "/auth/verify-code",
            {"phone_number": phone, "code": code, "full_name": "Load Test User"},
        )
        if session.get("token"):
            tokens.append(session["token"])
    print(f"{len(tokens)} ready")
    return tokens


def measure(concurrency: int, seconds: int, one: "Callable[[], tuple[int, float]]") -> dict:
    """Run `one` in `concurrency` threads for `seconds`, and time every call.

    Percentiles, not an average: an average hides the stalls, and a stall is
    what a person standing at a locker actually feels.
    """
    latencies: list[float] = []
    codes: dict[int, int] = {}
    guard = threading.Lock()
    stop_at = time.perf_counter() + seconds

    def worker() -> None:
        mine: list[tuple[float, int]] = []
        while time.perf_counter() < stop_at:
            status, took = one()
            mine.append((took, status))
        # Gathered at the end rather than per request: taking a lock a
        # thousand times a second measures the harness, not the server.
        with guard:
            for took, status in mine:
                latencies.append(took)
                codes[status] = codes.get(status, 0) + 1

    began = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        for _ in range(concurrency):
            pool.submit(worker)
    elapsed = time.perf_counter() - began

    latencies.sort()

    def pct(p: float) -> float:
        return latencies[min(int(len(latencies) * p), len(latencies) - 1)] * 1000 if latencies else 0.0

    good = sum(n for code, n in codes.items() if code == 200)
    return {
        "concurrency": concurrency,
        "requests": len(latencies),
        "rps": len(latencies) / elapsed if elapsed else 0,
        "ok": good,
        "bad": len(latencies) - good,
        "codes": codes,
        "p50": pct(0.50),
        "p95": pct(0.95),
        "p99": pct(0.99),
        "worst": (latencies[-1] * 1000) if latencies else 0,
    }


def reading(tokens: list[str], key: str, phone: str):
    """A student opening the app, a cabinet asking for work, a shipper looking up."""
    def one() -> tuple[int, float]:
        roll = random.random()
        if roll < 0.80:
            status, _, took = call("/parcels", token=random.choice(tokens))
        elif roll < 0.95:
            status, _, took = call("/cabinet/commands", key=key)
        else:
            status, _, took = call(f"/cabinet/receiver?phone_number={urllib.parse.quote(phone)}", key=key)
        return status, took
    return one


def writing():
    """The write path, which is the one that queues.

    Every write goes through one SQLite connection behind one lock, so this is
    the ceiling that matters. `request-code` is the honest thing to hammer: it
    writes a row, needs no token, and a fresh number each time means no rate
    limit and no shared row to contend on.
    """
    def one() -> tuple[int, float]:
        phone = "+849" + "".join(random.choice("0123456789") for _ in range(8))
        status, _, took = call("/auth/request-code", {"phone_number": phone})
        return status, took
    return one


def tidy(since_ms: int) -> None:
    """Delete what this run made, and nothing else.

    **Not optional politeness.** Every `request-code` this tool sends leaves a
    row in `otp`, and `otp` is what the hourly cap counts (BUG-015). A write
    run at 300 leaves eleven thousand of them, so the cap - sized for a real
    cohort - reads as spent and refuses real people for the rest of the hour.
    The first time it happened the load test broke the check that runs straight
    after it, which is the tool breaking the thing it was measuring.

    Bounded by time, not by phone prefix: the numbers are random and share
    `+849` with every real Vietnamese mobile, so a prefix delete would take
    real rows with it.
    """
    db = sqlite3.connect(DB)
    n = db.execute("DELETE FROM otp WHERE last_sent_at >= ?", (since_ms,)).rowcount
    r = db.execute(
        "DELETE FROM receivers WHERE full_name = 'Load Test User' AND created_at >= ?",
        (since_ms,),
    ).rowcount
    db.commit()
    db.close()
    print(f"cleaned up: {n} codes and {r} test accounts this run made")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--at", type=int, action="append", help="one concurrency level; repeatable")
    parser.add_argument("--seconds", type=int, default=10)
    parser.add_argument("--writes", action="store_true", help="hammer the write path instead")
    args = parser.parse_args()
    levels = args.at or [50, 200, 500]

    status, _, _ = call("/health")
    if status != 200:
        sys.exit("the server is not answering - ./gradlew :server:run")

    started = int(time.time() * 1000)
    atexit.register(tidy, started)

    if args.writes:
        print(f"{'at':>6} {'req':>8} {'req/s':>9} {'p50 ms':>9} {'p95 ms':>9} {'p99 ms':>9} {'worst':>9} {'errors':>8}")
        print("-" * 76)
        for level in levels:
            r = measure(level, args.seconds, writing())
            print(
                f"{r['concurrency']:>6} {r['requests']:>8} {r['rps']:>9.0f} "
                f"{r['p50']:>9.1f} {r['p95']:>9.1f} {r['p99']:>9.1f} {r['worst']:>9.1f} {r['bad']:>8}"
            )
            if r["bad"]:
                print(f"       status codes {r['codes']}")
        return

    key = cabinet_key()
    tokens = make_accounts(ACCOUNTS)
    if not tokens:
        sys.exit("could not make any accounts")

    # A number that exists, so the lookup does the same work it would in life.
    db = sqlite3.connect(f"file:{DB}?mode=ro", uri=True)
    phone = db.execute("SELECT phone FROM receivers LIMIT 1").fetchone()[0]
    db.close()

    print()
    print(f"{'at':>6} {'req':>8} {'req/s':>9} {'p50 ms':>9} {'p95 ms':>9} {'p99 ms':>9} {'worst':>9} {'errors':>8}")
    print("-" * 76)
    results = []
    for level in levels:
        r = measure(level, args.seconds, reading(tokens, key, phone))
        results.append(r)
        print(
            f"{r['concurrency']:>6} {r['requests']:>8} {r['rps']:>9.0f} "
            f"{r['p50']:>9.1f} {r['p95']:>9.1f} {r['p99']:>9.1f} {r['worst']:>9.1f} {r['bad']:>8}"
        )

    print()
    for r in results:
        if r["bad"]:
            print(f"  at {r['concurrency']}: status codes {r['codes']}")

    top = results[-1]
    print()
    print(f"at {top['concurrency']} in flight: {top['rps']:.0f} req/s, "
          f"half served under {top['p50']:.0f} ms, 99 in 100 under {top['p99']:.0f} ms, "
          f"{top['bad']} failed")


if __name__ == "__main__":
    main()
