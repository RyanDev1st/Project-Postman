# ADR 0019 — We build the server ourselves

- **Status:** accepted
- **Date:** 2026-08-15
- **Decided by:** Ryan. He is the only CSE student on the team. The Server team
  are ECE students, and the server is not the part they are able to build
- **Reverses:** the division of labour in `CLAUDE.md` and in
  [architecture.md](../reference/architecture.md) sections 3 and 4, which said
  the Server team owns the API and the database and that we do not change it
- **Relates to:** [ADR 0005](0005-we-propose-they-object.md) — we already write
  the contract and they object to it. This ADR extends that to the code behind it

## The problem

Every unticked task on the board needs a server, and the server does not exist.
P0-04 — *send the contract, get an address back* — has been the critical path
for sixteen tasks since Phase 0. It cannot close, because the thing it waits for
is not being built.

On 2026-08-15 the Server team handed over everything they have: an ESP32 sketch,
`main.py`, `models.py`, `database.py`, and two screenshots of it running. That
is enough to judge, and this ADR is the judgement.

## What they have

It runs. The screenshots show FastAPI serving `GET /lockers/3` on
`127.0.0.1:8000` and answering
`{"locker_id": 3, "locker_number": "LK-LIB-03", "status": "empty"}` from a real
MySQL row, dated 2026-08-11. The ESP32 reports a door state to it over the LAN.
That is a working service, and it is more than nothing.

It is not a foundation. Three things, in order of how much they matter.

**1. A parcel does not belong to anybody.** `Package` has `package_id`,
`tracking_code` and `status`, and no owner column. `Delivery` joins a package to
a locker, and never to a user. No table in the schema can answer *is this parcel
yours* — the single question the whole product turns on, and the question the
pickup flow exists to ask.

**2. Nothing is authenticated.** No endpoint takes a token. `PUT
/lockers/{id}?status=...` lets anyone on the same WiFi mark any locker any
state, and the ESP32 calls it that way because that is the only way there is.
There is no open-door endpoint yet, so the dangerous one is still ahead.

**3. A user has no phone number.** `User` holds `student_id`, `full_name` and
`email`. Our flow starts with a shipper typing a **phone number** on the cabinet
screen ([ADR 0003](0003-parcel-locker-product.md)). The two models do not meet.

These are not signs of bad work. They are signs of a different job: their schema
was built to *show lockers on a dashboard*, and ours has to *refuse a stranger*.

## The decision

**We build the server. The Server team keeps the hardware.**

The split moves to where the skill is:

| Us | Server team |
| --- | --- |
| The API, the database schema, and every rule that decides whether a door opens | The ESP32, the relays, the wiring, and the cabinet itself |
| The phone app and the cabinet screen, as before | Reporting a door state up to our API, and opening on our command |

What we take from their work is the **shape**, not the code:

- **FastAPI + SQLAlchemy + MySQL.** A reasonable stack, they know it, and it
  keeps them able to read what we write. Kept.
- **`LK-LIB-03`** — site and number in the locker name. Kept, because it reads
  the same to a person and to a query.
- **The LAN assumption.** Their ESP32 posts to a server on the local network.
  That answers the hosting problem this project has had all along: no host, no
  card, and [no card-gated services](0014-openfreemap-not-google.md) is a
  standing rule. A machine on the university network can serve the cabinets in
  front of it.

None of their files are copied into this repo. `database.py` carries a live
database password in plain text, and nothing from that file may enter a commit.

## What this costs

**The board gets longer and Phase 5 gets further away.** We are now writing the
thing that was going to be handed to us. P0-04 as written — *send the contract,
get an address back* — can never close, because there is nobody to send it to.
It becomes *stand the server up and give the app an address*.

**Two front-ends and a back-end, one student.** The mitigation is that the
contract is already written and already argued over
([api-contract.md](../reference/api-contract.md)), so the server is being built
against a specification rather than invented. The rules the server must enforce
on a scanned code are written down there in full, and they were written before
we knew we would be the ones enforcing them.

**We inherit the security burden entirely.** Every check in the phone app runs
on the caller's phone and protects nobody. Until now the honest answer was *the
server has to do this and the server is not ours*. That excuse is gone.

## Checked

Nothing yet. The decision is a week old at the point it is written, and there is
no server standing up. The first evidence that this ADR was right will be
P5-10 — four refused attempts, read from our own server log.
