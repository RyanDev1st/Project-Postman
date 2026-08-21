Parent: ../reference/architecture.md

# Can this survive being on a real network, 2026-08-21

Ryan asked for a production-ready server on a current stack that carries 300
users at once and stands up to the common web attacks - the Cloudflare and
Tripwire lists.

## Status

**Load: answered, with room.** 300 concurrent readers get 5,488 requests a
second, 99 in 100 served inside 135 ms, zero errors. The binding constraint on
this product is 25 boxes, not the server.

**Attacks: the host-level ones are closed and measured.** Injection, XSS,
brute force, oversized bodies, slow requests, missing headers, and per-caller
flooding are each tested below.

**A real distributed denial of service is not closed and cannot be by code
here.** That is decided upstream by whoever runs the network. Anything else
would be a lie told in a config file. See *What this does not cover*.

## Scope

The server and the cabinet screen. The Android app's own hardening is not in
scope here. Nothing was measured on a phone; the load numbers are from one
laptop against itself, which flatters latency and does not flatter throughput.

## Evidence

### The stack is already current

Checked against Maven Central the same day, not from memory:

| | pinned | latest released |
| --- | --- | --- |
| Ktor | 3.2.0 | 3.2.0 |
| sqlite-jdbc | 3.49.1.0 | 3.49.1.0 |

No upgrade was needed and none was made. Kotlin is 2.4.10.

### Load, at the number that was asked for

`scripts/stress.py`, keep-alive connections, real accounts with real tokens,
against the real SQLite file.

| in flight | requests | req/s | p50 | p95 | p99 | worst | errors |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 50 | 63,632 | 5,286 | 8.5 ms | 17.8 ms | 24.0 ms | 118 ms | 0 |
| 150 | 66,681 | 5,526 | 23.4 ms | 50.6 ms | 67.4 ms | 215 ms | 0 |
| **300** | **66,431** | **5,488** | **39.3 ms** | **97.9 ms** | **134.9 ms** | **406 ms** | **0** |

Writes at 300 in flight: 1,097 req/s, p99 561 ms, zero errors.

**Throughput is flat from 50 to 300 and latency rises in proportion.** That is
a saturated server sharing itself out fairly, not one falling over: the work
per request is constant and the queue is doing its job. A server about to
collapse shows throughput *falling* as concurrency rises.

The database is one SQLite connection behind one global lock, which is the
obvious thing to suspect. At these numbers it is not the problem - 5,488 reads
a second through one lock is far past a campus. If it ever becomes the
problem, WAL already allows concurrent readers and a read pool is the fix.

**Measured with the per-caller ceilings raised, on purpose.** All 300 came
from one address, so the ceilings would have measured the limiter rather than
the server. They were restored immediately after and the whole journey re-run.

### The attacks, one at a time

| Attack | State | How that is known |
| --- | --- | --- |
| SQL injection | **Closed** | Every statement is a `PreparedStatement` with bound arguments. There is exactly one place that builds SQL from a value - `PRAGMA user_version = $n`, an `Int` from our own list - and it carries a comment saying so |
| XSS | **Closed** | The cabinet screen contains no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `document.write` or `eval`. CSP `default-src 'none'` on every API reply |
| Clickjacking | **Closed** | `X-Frame-Options: DENY` and `frame-ancestors 'none'` |
| MIME sniffing | **Closed** | `X-Content-Type-Options: nosniff` |
| Credential brute force | **Closed** | Five tries per code, ever, and the counter survives a restart because it is a row (BUG-013). Wrong, expired and spent all answer the same |
| SMS pumping | **Closed** | Per-number cooldown, per-address ceiling, and an hourly cap across everybody (BUG-015) |
| Oversized body | **Closed** | 16 KB ceiling, checked on the declared length before the body is read. A 40 KB body is refused `413` |
| Slow request (slowloris) | **Closed** | 10 s to finish sending. A well-formed POST that announces 200 bytes and sends 20 gets `408 Request Timeout` at 10.0 s, measured |
| Flooding one endpoint | **Bounded** | Per-caller ceilings. Under production limits a write flood of 19,268 requests was 611 accepted, 18,657 refused `429` |
| Server fingerprinting | **Closed** | The `Server` header is blanked. It named Ktor and its version, which tells a stranger which advisories to read |
| Stack traces on the wire | **Closed** | `StatusPages` answers a code and nothing else; the trace goes to the log |
| Session hijack | **Bounded** | HTTPS only, both front-ends refuse plain HTTP, tokens are 256 bits from `SecureRandom`, compared with `MessageDigest.isEqual` |
| Distributed denial of service | **NOT closed** | See below. Not closable here |

### Two faults found in the hardening itself, before it shipped

**HSTS would have killed the cabinet screen.** `Strict-Transport-Security` is
remembered per host, not per port. The server is `127.0.0.1:8443` and the
screen is served from `127.0.0.1:8137` over plain HTTP - the same host. Sending
the header would have made the browser force HTTPS on the screen's own address,
where nothing speaks TLS, and remember it for a year on that machine. It is off
behind `send_hsts`, to be turned on in the same change that puts a real
certificate and a real domain in front.

**Address-keyed limits would have punished the intended crowd.** A university
NAT presents a whole building as one address. A read ceiling keyed by address
means students rate-limit each other, and the busiest hour looks exactly like an
attack. Reads are keyed by the token, cabinet traffic by the cabinet key, and
only asking for a code is by address - because nobody has a token yet.

### And one in a number set earlier the same day

The hourly OTP cap was 200, chosen when the only concern was the SMS bill.
Against the 300 users this is meant to carry, 200 is *below* one orientation
day: 300 students with two retries each is 900. It would have refused real
students for the rest of the hour. Now 1,000, and the reasoning is written
beside the number rather than in a commit nobody reads.

`stress.py` was also leaving eleven thousand codes behind after every write
run, which spent that same cap and broke the check that runs immediately
after - the measuring tool breaking the thing it measured. It deletes what it
creates now, bounded by time rather than by phone prefix, because the numbers
it invents share `+849` with every real Vietnamese mobile.

## What this does not cover

1. **A real DDoS.** Everything above is host-level: it stops one machine, or a
   few, from occupying the server. A botnet saturating the campus uplink is
   decided by whoever runs that uplink. The honest fix is a service in front -
   Cloudflare's free tier is the usual answer and needs a domain. **This is the
   one item on the brief that code in this repo cannot deliver.**
2. **Malware.** Nothing here accepts a file upload, so there is no scanning
   surface. If parcel photographs are ever added, that changes and this line
   must be revisited.
3. **The app.** Certificate pinning, root detection, and tamper checks are not
   done and are not obviously worth it for a campus locker.
4. **Codes in the database.** A six-digit code is stored as a bare unsalted
   SHA-256, which is a million guesses - about a second. Anyone holding
   `data/locker.db` can recover a live 48-hour backup code. The fix is an HMAC
   with a key kept outside the database. **Not done**, and it is the largest
   thing left on this list.
5. **No backup of `data/locker.db`.** One file, one machine, no copy.

## Next

1. **Put something in front of the server before it is public.** A Cloudflare
   quick tunnel needs no account and no card and would also end the
   self-signed certificate problem for every tester at once.
2. **Pepper the short codes** - HMAC with a key from the environment, so the
   database file alone is not enough. Changes `checkloop.py` and `stress.py`,
   both of which currently recover codes by brute force, and that is a feature
   of the fix rather than a cost of it.
3. **Copy the database somewhere.** Any copy beats none.
4. **Re-run `scripts/stress.py --at 300` after any change to a route**, and
   read the error column, not just the rate.
