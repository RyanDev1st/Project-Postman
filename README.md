# Project Postman — VGU Smart Locker (app side)

A parcel drop-off locker. A shipper leaves a parcel in a box. The receiver picks it up with a phone. Nobody needs a key.

This repo holds the **IT team** work — two front-ends:

- **The phone app**, for the receiver. Register, get told a parcel arrived, scan the cabinet, open the box.
- **The cabinet screen**, for the shipper. Find the receiver, drop the parcel. No login, nothing to install.

The Server team owns the API and the database in a separate repo. Our front-ends call their API and decide nothing on their own.

## How it works, in six steps

1. The receiver registers in the app with a phone number.
2. The shipper types that number on the cabinet screen and picks the masked name.
3. The server opens a free box. The shipper puts the parcel in and closes the door.
4. The receiver's phone gets a notice: a parcel is waiting, in which box.
5. The receiver walks up and scans the QR shown on the cabinet screen.
6. The server checks who they are, opens their box, and records it.

## Where things are

| I want to… | Go to |
| --- | --- |
| Know how we work — read this first | [`docs/reference/working-rules.md`](docs/reference/working-rules.md) |
| See the plan and what is left | [`docs/roadmap/README.md`](docs/roadmap/README.md) |
| See what we must agree with the Server team | [`docs/reference/api-contract.md`](docs/reference/api-contract.md) |
| Understand the parts of the system | [`docs/reference/architecture.md`](docs/reference/architecture.md) |
| Look up a word I do not know | [`docs/reference/glossary.md`](docs/reference/glossary.md) |
| Report or track a bug | [`docs/reference/bug-log.md`](docs/reference/bug-log.md) |
| Know why we chose something | [`docs/adr/`](docs/adr/) |
| Read all docs | [`docs/README.md`](docs/README.md) |

## Status

**Phase 0 — Agree.** `0 / 60` tasks. No app code yet. The tech stack is not chosen — see [ADR 0001](docs/adr/0001-tech-stack.md).

Do not start Phase 1 until every Phase 0 task is done. Phase 0 is the cheapest phase and it prevents the most rework.

**The most urgent open question: does the cabinet have its own network connection?** Every part of this plan assumes it does. That is task **P0-01**.

## For agents

Read [`CLAUDE.md`](CLAUDE.md) first. It holds the folder rules, the file-size cap, the tick protocol, and the delivery rules.
