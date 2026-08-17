Parent: architecture.md

# What the cabinet has to do

For whoever writes the software on the Pi. It is the whole job, and it is smaller than it looks.

The hardware it is written against is the proposal in [ADR 0021](../adr/0021-build-for-the-proposed-hardware.md) — a Pi with a 1024x600 touchscreen, a 32-channel RS485 relay board driving 25 solenoid locks, and an ESP32 reading 25 infrared item sensors and reporting to the Pi.

## Status

Written 2026-08-17, against a bill of materials nobody has bought yet. Every endpoint below exists and runs today; `scripts/cabinet-agent.py` is a working example of the polling half, in about 140 lines of Python, and was used to walk a parcel from a shipper to a receiver on one laptop.

## The one rule

**The cabinet decides nothing.**

Not which box. Not whose parcel. Not whether a code is good. It is asked to open box 07 and it opens box 07. Everything else is the server's, and a cabinet that decided any of it would be a second place the rules live — and the second place is always the one that disagrees.

The cabinet knows three things the server cannot: whether a relay fired, whether a box has something in it, and whether the network is up. It reports those. That is all.

## Scope

| Runs on | Does |
| --- | --- |
| **Pi** | the screen, the network, the relays, and every call below |
| **ESP32** | reads 25 infrared sensors, tells the Pi which boxes are full. **No network, no server calls, no key** |

The ESP32 not having the key is deliberate. One key per cabinet ([ADR 0004](../adr/0004-one-key-per-cabinet.md)), and a key on a microcontroller with no storage protection is a key on a bench.

## Talking to the server

Base URL and key go in a config file placed on the Pi at setup. Never in the source, never in git.

Every call carries the key:

```
X-Cabinet-Key: <the cabinet key>
```

**HTTPS only.** There is no flag to turn that off and there will not be one. A cabinet that would speak plain HTTP under some condition is a cabinet somebody will find that condition for.

Today the certificate is self-signed (`config/dev-cert/locker.crt`) and the Pi must be told to trust that one file. A real cabinet gets a real certificate.

## The loop

Three calls. Nothing else.

### 1. Ask for work — about once a second

```
GET /cabinet/commands
→ { "commands": [ { "id": "PEyfLI...", "box": "07", "action": "open" } ], "at": "..." }
```

Usually the list is empty. That is normal and is not an error.

**A command comes back exactly once.** The server stamps it as taken in the same transaction that hands it over, so a Pi that loses the reply and asks again does not open the door twice. Do not build a cache to guard against that; it is already guarded, and a second guard that disagrees is worse than none.

A command older than 90 seconds is dropped by the server and never delivered. A door that opens two minutes after the student gave up and walked away is worse than one that never opened.

### 2. Say it happened

```
POST /cabinet/command-done
{ "id": "PEyfLI...", "result": "ok" }
```

Send this as soon as the relay has fired — not after the door shuts. They are two different facts and one of them you cannot see.

Send it when it fails too. `result` is free text; say what happened.

### 3. Say the door is shut

```
POST /cabinet/door-closed
{ "box_number": "07", "purpose": "collect" }
```

This is the call that finishes a parcel. On `collect` the server writes the parcel off and frees the box; on `drop` it does neither.

**Exactly one thing sends it per open.** The screen can send it - the shipper is standing there and taps a button - and so can the Pi. Both doing it writes the same event twice and makes the log say two doors closed where one did. Seen in a test run on 2026-08-17, where the screen and the stand-in both reported the same drop. Decide which one owns it for your build and let the other stay quiet.

**`purpose` is the open question — see [BUG-009](bug-log.md).** Nothing in call 1 tells you which kind of open it was, so today you cannot know. Do not guess. Two ways out, and the second is better:

- the server starts telling you in call 1, or
- **the item sensor tells you.** A box that went from empty to full was a drop. A box that went from full to empty was a collect. That is not an inference about what somebody intended — it is the thing that actually happened, which is better evidence than either of us had before.

If you are building to this document before that is settled, build the sensor path. It is the one that survives.

## What the sensors are for

25 infrared sensors, one per box, read by the ESP32 and reported to the Pi.

They answer a question this system has never been able to answer. From [ADR 0006](../adr/0006-no-sensor.md): a driver can open a door, walk away still holding the parcel, and a delivery gets recorded that never happened. With no sensor there was no way to know, and the risk was accepted in writing.

An infrared sensor in a box reports **whether the box has something in it**. It does not report whether the door is shut. Keep those apart — the second is tidiness, the first is the one that was hard.

Useful shapes, in the order they are worth building:

1. **Which way an open went** — the `purpose` problem above, solved by watching the box rather than asking.
2. **A drop that did not happen** — door opened, box still empty when it shut. The server should hear about that.
3. **A box that says free but is not** — the cabinet believes box 12 is empty, the sensor sees something in it. Endpoint 14 `POST /cabinet/fault` already exists for a box that should be taken out of service.

None of these are built. They need hardware to test on, and a claim about a sensor that has never been wired up is a claim nobody should act on.

## Driving the locks

Ours to state, not ours to design: the relay board is RS485 Modbus RTU, the locks are 12V solenoids, and neither is visible to the server. The server sends a box number. How that becomes a channel on a relay board is entirely the Pi's business.

Two things that are not negotiable:

- **A relay fires because a command said so.** Never on a timer, never on a retry, never on a screen refresh. One command, at most one door.
- **A solenoid is a pulse, not a state.** Holding 12V on a lock coil to keep a door open cooks the coil.

## When the network goes

The bill of materials has a 4G dongle and a note saying to use the campus LAN instead. Assume it drops either way.

Today: keep polling, and say so on the screen. Do not queue opens locally and do not invent one — every open the server issued that you did not collect within 90 seconds is dead, and correctly so.

There is an agreed offline story — endpoints 16 and 17, `offline-secret` and `reconcile` — and it is not built on either side. Do not build half of it.

## What the screen shows

The Pi runs the cabinet screen from `src/cabinet/` in a browser, full screen, at 1024x600.

**It is a public terminal.** Anyone can walk up to it. It never shows a full name, a phone number, or a parcel list. That rule is in [PRODUCT.md](../../PRODUCT.md) and it is not a preference.

## The whole contract

Every endpoint, with its exact fields and refusal codes, is in [api-contract.md](api-contract.md). Numbers 9 to 14 and 22 to 23 are the cabinet's. A change to any of them needs both teams.
