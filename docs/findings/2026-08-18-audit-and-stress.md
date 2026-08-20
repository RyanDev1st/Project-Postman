Parent: ../reference/architecture.md

# Audit and load test, 2026-08-18

An overnight pass on the server: what breaks, what holds, and how much it
carries. No phone and no cabinet were in the room — see
[real-world-test.md](../reference/real-world-test.md) for why that is a real
limit and what it does not cover.

## Status

Done. Three faults found and fixed, each with a check kept behind it. One
weakness found and **not** fixed, because the fix is a decision rather than a
patch. The load question is answered: 500 concurrent is comfortable, and the
first answer was wrong.

## Scope

- The whole parcel journey, end to end, against the real endpoints over TLS
- The refusal paths: wrong codes, no token, wrong key, lockouts, replay
- Throughput and latency at 50, 200 and 500 requests in flight
- What the app and the cabinet screen say about themselves

Not covered: anything only a device can show. No camera read a QR, no thumb
touched a screen, no door moved.

## Evidence

### The loop works, with no device

```
python scripts/checkloop.py
```

Twenty checks, all green. It registers a receiver, drops a parcel to them,
works the cabinet, lists the parcel, scans, collects, and reads the database
after every step. The two that matter most:

```
OK   4b. the full name and the number are not on the public screen  -  Tran T. T***
OK   8c. the same code with no token opens nothing  -  HTTP 401
```

The second is the photograph case from [ADR 0003](../adr/0003-parcel-locker-product.md).
A picture of the cabinet screen opens nothing, because the QR is not a key.

### BUG-013 — any account could be taken over by guessing

The one-time code is six digits, and five wrong tries were supposed to end it.
They did not.

`Otp.verify` deleted the whole `otp` row when the tries ran out. That row is
also where `last_sent_at` lives, and it is the only thing the
one-code-a-minute cooldown is counted from. No row, no cooldown — so a new
code could be asked for immediately, with five fresh tries:

```
five wrong tries burned.
attacker asks for another code: HTTP 200 OK             <- before
attacker asks for another code: HTTP 429 RATE_LIMITED   <- after
```

Ask, guess five, ask, guess five, with nothing in the way. A million codes at
the rate measured below is minutes of work, and the prize is somebody else's
account and whatever is sitting in their box.

Every line of that function is right on its own. The fault is only visible
once you notice the row is doing two jobs and one of them ends sooner than the
other — which is why probing found it and reading did not.

The row is now emptied rather than deleted. Five guesses a minute is about
four years for a million. `checkloop.py` step 3c is that attack, kept.

### What held

Probed the same way, and these were fine:

| Checked | Result |
| --- | --- |
| Cabinet endpoints with no key, or a wrong key | `401` both |
| App endpoints with no token, or a wrong token | `401` both |
| A cabinet key used on `/parcels` | `401` — the cabinet cannot read anybody's parcels |
| The typed backup code, five wrong tries | Locked 15 minutes, counter on disk |
| Hammering a locked box 20 more times | `BOX_LOCKED_OUT` every time; the lock neither extended nor wore off |
| Replaying a session code | Refused by a primary key on `session_use`, not by a check somebody has to remember |
| Wrong, expired and already-spent codes | All answer the same thing, so no code tells you whether it exists |

`session_use` rows outliving their session is worth keeping: a used code stays
used after it expires, so a replay does not start working again the moment the
session row is swept.

### Not fixed — the codes are recoverable from the database

`Ids.hash` is a bare unsalted SHA-256. A six-digit code is a million
possibilities, so anybody who can read `data/locker.db` can recover **every
live one-time code and every live backup code** in about a second. This test
suite does exactly that, on purpose, to read its own code back:

```
OK   2. the code was recovered from its hash  -  097099
```

For a code that lives five minutes this is close to harmless. For the typed
backup code it is not: those live 48 hours, and recovering one opens a box.

Not fixed because the fix is a decision. The honest options are a per-row salt
with a slow hash, or accepting it and writing down why. Either way
`data/locker.db` should be treated as a secret — and it has no backup either.

### How much it carries

```
python scripts/stress.py --seconds 10
python scripts/stress.py --writes --seconds 10
```

| Load | In flight | req/s | p50 | p95 | p99 | Failed |
| --- | --- | --- | --- | --- | --- | --- |
| Reads | 50 | 6,977 | 6.5 ms | 13.7 ms | 17.9 ms | 0 |
| Reads | 200 | 6,703 | 25.7 ms | 58.5 ms | 78.0 ms | 0 |
| Reads | 500 | 6,381 | 67.8 ms | 165.4 ms | 222.5 ms | 0 |
| Writes | 500 | 1,892 | 245.2 ms | 365.5 ms | 413.6 ms | 0 |

Throughput is flat from 50 to 500 while latency climbs in step. That is what a
healthy saturated server looks like: it queues, it does not thrash, and
nothing is dropped. The only non-200s anywhere were `429`s from the rate
limiter refusing a number it had just seen, which is it working.

**500 in flight is a much heavier thing than 500 students.** A student opens
the app, sends one request, then reads the screen for half a minute. At 6,381
req/s the server would serve all 5,000 of VGU's students opening the app
inside a second.

The single SQLite connection behind one lock caps writes near 1,900 a second
and does not fall over. A drop or a collect is a handful of writes. **The
binding constraint on this product is 25 boxes in a cabinet, not the server.**

### The first load number was wrong, and believable

Every endpoint first answered at about 1,250 req/s — including `/health`,
which touches no database at all. That was not the server's ceiling. It was
the cost of 1,250 TLS handshakes a second on this laptop, because `urllib`
opens a new connection per request and real clients do not:

```
/health  at 500, new connection each time:  1,288 req/s
/health  at 500, connection held open:      7,286 req/s
```

Worth writing down because the wrong number was plausible and would have
started an optimisation nobody needed. The harness was the bottleneck, and it
was measuring itself.

### Smaller things

- **The cabinet screen said `Cabinet not set`** while signing its calls as
  `vgu-back-gate`. Two places hold a cabinet id, and the footer read the one
  that is null on purpose. Fixed.
- **`%1$d of 20 doors free` hardcodes a total the app was never told**, and
  the bill of materials says 25. It never renders today — `freeDoors` has no
  caller and defaults to empty — so it is dead copy, reported and left alone
  rather than changed. Same for `All 20 boxes in use`.
- **`config/dev-cert` names one address and is written once.** This laptop
  moved from `192.168.1.19` to `172.16.0.2` during the night, which
  `checktest.py` caught. Nothing was regenerated: deleting that folder also
  invalidates the certificate installed on any phone, and that is Ryan's call.

## Next

1. **Decide on the code hashes.** Salt and slow them, or write down why 48
   hours of recoverable backup codes is accepted. The backup code matters more
   than the one-time code.
2. **Decide what happens to `data/locker.db`.** It holds every hash, and there
   is no backup of it.
3. **BUG-009 still needs a word** — endpoint 22 carries the purpose, or the
   item sensors answer it by watching the box. Nothing else in the contract
   can tell a drop from a collect.
4. **BUG-011 and BUG-012 need one run on Ryan's phone to close.** Both are
   fixed and proven against the real endpoints, but both were found on a
   device and they close on the device that found them.
5. **The certificate will keep going stale.** A Cloudflare quick tunnel does
   not care what address the laptop has, costs nothing, and needs no account.
