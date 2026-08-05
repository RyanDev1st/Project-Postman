# Docs index

One line per doc. Add a row when you add a doc. Move a row to **Legacy** when the doc is superseded.

## Roadmap — the plan and the task board

| Doc | What it holds |
| --- | --- |
| [roadmap/README.md](roadmap/README.md) | Master board. Task format, tick rules, phase counts, current phase |
| [roadmap/phase-0-agree.md](roadmap/phase-0-agree.md) | Decide the stack, the barcode format, the API contract |
| [roadmap/phase-1-skeleton.md](roadmap/phase-1-skeleton.md) | Empty app runs on both phones and reaches the server |
| [roadmap/phase-2-login.md](roadmap/phase-2-login.md) | Login for both account types, plus error states |
| [roadmap/phase-3-see-lockers.md](roadmap/phase-3-see-lockers.md) | Locker points, free counts, nearest point, locker list |
| [roadmap/phase-4-open-locker.md](roadmap/phase-4-open-locker.md) | Open and lock a locker. The core feature |
| [roadmap/phase-5-barcode.md](roadmap/phase-5-barcode.md) | Card scan, plus ID + password fallback |
| [roadmap/phase-6-modes.md](roadmap/phase-6-modes.md) | Normal mode and exam-season mode |
| [roadmap/phase-7-handoff.md](roadmap/phase-7-handoff.md) | Parcel handoff. Marked future. Do last |
| [roadmap/phase-8-ship.md](roadmap/phase-8-ship.md) | Device tests, bug fixing, release build |

## Reference — how the system works **now**

Edit these in place. No date in the name.

| Doc | What it holds |
| --- | --- |
| [reference/architecture.md](reference/architecture.md) | The five parts of the system and how they connect |
| [reference/api-contract.md](reference/api-contract.md) | Every request the app sends and every answer it expects. **Draft** |
| [reference/glossary.md](reference/glossary.md) | Plain-English meaning of every term used in this repo |
| [reference/bug-log.md](reference/bug-log.md) | Running list of bugs found on real devices |

## ADR — why we chose something

One short file per non-obvious decision. Never edited after it is `accepted`. Reverse it with a new ADR.

| Doc | Decision | Status |
| --- | --- | --- |
| [adr/0000-template.md](adr/0000-template.md) | Template. Copy it | — |
| [adr/0001-tech-stack.md](adr/0001-tech-stack.md) | Which tech the app is built with | **open** |

## Findings — dated reports

One file per report: `findings/YYYY-MM-DD-<topic>.md`. Never edited after the day. See [findings/README.md](findings/README.md).

*(none yet)*

## Legacy — superseded docs

Moved here, never deleted. See [legacy/README.md](legacy/README.md).

*(none yet)*
