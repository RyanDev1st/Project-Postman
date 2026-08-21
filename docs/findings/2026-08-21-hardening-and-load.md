Parent: ../reference/architecture.md

# Can this survive being on a real network, 2026-08-21

Ryan asked for a production-ready server on a current stack that carries 300
users at once and stands up to the common web attacks - the Cloudflare and
Tripwire lists.

## Status

**Load: answered, with room, and the room is bigger than the first number
said.** At 300 concurrent the server was never the limit - the laptop
generating the load was. Split across four generators it carries **600
requests in flight at 7,413 a second**, worst p99 153 ms, no error the server
produced. The binding constraint on this product is 25 boxes.

**Attacks: the host-level ones are closed and measured.** Injection, XSS,
brute force, oversized bodies, slow requests, missing headers, and per-caller
flooding are each tested below.

**A stolen database file no longer reads a live pickup code.** That was the
largest thing left open this morning and it is closed - measured both ways, on
the real database. See *The database file on its own*.

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

| in flight | generators | req/s | p50 | p99 | errors the server made |
| --- | --- | --- | --- | --- | --- |
| 50 | 1 | 5,286 | 8.5 ms | 24.0 ms | 0 |
| 150 | 1 | 5,526 | 23.4 ms | 67.4 ms | 0 |
| **300** | 1 | **5,488** | 39.3 ms | 134.9 ms | 0 |
| 300 | 2 | **6,414** | 42 ms | 128 ms | 0 |
| **600** | 4 | **7,413** | 77 ms | 153 ms | 0 |

Writes at 300 in flight: 1,097 req/s, p99 561 ms, zero errors.

**The single-figure answer is wrong and the reason matters.** One generator at
300 gives 5,488 a second. Two generators at 150 each - the same 300 in flight -
give 6,414. The load generator is Python on the same laptop, and it saturates
before the server does, so every one-process number here is a floor on the
server rather than a measurement of it. Four generators reach 7,413 a second
at 600 in flight and the server still answers everything.

The 506 non-200s in the four-generator run were all `404`, and all
self-inflicted: each generator deletes its own test accounts when it finishes,
which deletes the accounts the others are still asking about. No `5xx`, no
timeouts, no refused connections, in any run.

**Throughput rises with concurrency and latency rises in proportion.** That is
a server sharing itself out fairly. A server about to collapse shows
throughput *falling* as concurrency rises, and this one never did - it was
still climbing when the measuring laptop ran out.

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
| Reading codes out of a stolen database | **Closed** | Digests are keyed - see below. A million guesses against the real file recover nothing |
| Session hijack | **Bounded** | HTTPS only, both front-ends refuse plain HTTP, tokens are 256 bits from `SecureRandom`, compared with `MessageDigest.isEqual` |
| Distributed denial of service | **NOT closed** | See below. Not closable here |

### The database file on its own

The audit of 2026-08-18 said a six-digit pickup code stored as a bare SHA-256
is a million guesses - about a second - so anybody holding `data/locker.db`
could read every live code out of it. That is now closed. `Ids.hash` is an
HMAC under a key that is deliberately not in the database file, argued in
[ADR 0022](../adr/0022-a-key-outside-the-database.md).

Run against the real file, with a real code the server had just issued:

| The attacker holds | Guesses tried | Result |
| --- | --- | --- |
| `data/locker.db` | 1,000,000 | 2 s, **nothing found** |
| `data/locker.db` **and** `config/pepper.key` | 823,388 | 5 s, code recovered |

Four unit tests hold the property in place, including the attack itself: build
the old lookup table, look the stored digest up in it, and require a miss.

**And the fix was half a regression until it was measured.** Calling
`Mac.getInstance` on every request took read throughput from 5,488 a second to
2,589, because each call walks the JCA provider list under a lock. One `Mac`
per thread fixed it. The hash costs **287 ns**, over a million calls - 0.2 %
of one core at the rates above. The first number was found by re-running the
load test after the change rather than by reasoning that a hash is cheap.

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
4. **Where the key is kept.** The codes are safe from the database file
   alone, but on this machine `config/pepper.key` sits on the same disk as
   `data/locker.db`. A real deployment sets `LOCKER_PEPPER` in the
   environment and has no file. Nobody has done that, because nothing is
   deployed.
5. **Backups are on the same disk.** `scripts/backup.py` exists and works -
   a consistent snapshot taken while the server runs, opened afterwards and
   checked. Nothing runs it on a schedule, and the default destination
   survives a mistake but not a dead disk.

## Next

1. **Put something in front of the server before it is public.** A Cloudflare
   quick tunnel needs no account and no card and would also end the
   self-signed certificate problem for every tester at once.
2. **Run `scripts/backup.py --to` another disk on a schedule**, and copy
   `config/pepper.key` once, by hand, somewhere the backups do not go. Neither
   is worth anything without the other and they must not travel together.
3. **Re-run `scripts/stress.py --at 300` after any change to a route**, and
   read the error column, not just the rate. Use more than one generator if
   the number itself matters - one is not enough to find the server's limit.
4. **Measure again on the Pi.** Every number here is from a 16-core laptop
   talking to itself. The Pi is the machine this runs on and nothing has been
   measured there.
