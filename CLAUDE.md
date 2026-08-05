# Project Postman — VGU Smart Locker — Agent Workspace

Project memory for Claude Code and other agents. Loaded every session. Keep **under 200 lines**.

## Mission

Ship a working smart-locker system for VGU. Students, lecturers, exam candidates and parents open lockers with a phone. Staff manage locker points, modes and history.

This repo holds the **IT team (app) side**. The Server team owns the API and database; we consume their API.

| In scope | Out of scope (unless user says otherwise) |
| --- | --- |
| Mobile app (Android + iOS), UI, login, locker open/close, barcode scan, offline handling | Server code, DB schema, locker hardware firmware |
| Integration with the real Server-team API | Fake/mock backends in production paths (test fixtures must be labelled) |
| | Secrets in repo, commits, or chat |

## Feature list (from team requirements)

App (this repo):
1. Screens: login, locker-point list, locker list, open/lock locker.
2. Login for 2 account types: student/lecturer, and candidate/parent.
3. Call Server API. Handle no-network and wrong-password cases.
4. Test on real Android/iOS phones. Log every bug and fix it.
5. Show locker points with free-slot count. Suggest the nearest point.
6. Scan the student/lecturer card barcode (or the code in the VGU Library app) to open a locker. Fallback: login with ID + password.
7. Future: fast parcel handoff — read scanned locker data, auto-pick a free locker, send the open code to the receiver.

Server team (not this repo, but we depend on it):
- Tables: users, locker, open/lock history, school events.
- API: login, issue password, open locker, read usage history.
- Auto-send login name + password by email to candidates/parents.
- Periodic backup; access rights per user group.
- Locker state per mode (exam-reserved vs shared), locker size, order data.
- Card barcode mapped to a user account.
- Many locker points; each point has its own code, locker list, state, mode.
- Two operating modes (normal / exam season) + admin mode switch.

## Repository map

| Path | Purpose |
| --- | --- |
| `CLAUDE.md` | Team agent instructions (this file) |
| `README.md` | Human entry point — status and where things are |
| `docs/` | Documentation. Index = `docs/README.md` |
| `docs/roadmap/` | The plan and the task board. One file per phase. Task IDs `P<phase>-<nn>` |
| `docs/reference/` | How the system works **now** — architecture, API contract, designs (edit in place) |
| `docs/findings/` | Dated audits / test reports (immutable; supersede by date) |
| `docs/legacy/` | Superseded docs (move here, never delete) |
| `docs/adr/` | One short file per non-obvious decision (the *why*) |
| `src/` | App source code — feature folders only |
| `tests/` | Tests mirroring `src/` |
| `scripts/` | Build and automation scripts |
| `.claude/` | Agent context, skills, path-scoped rules |

**Root policy:** only `CLAUDE.md`, `.gitignore`, setup files (`package.json`, `pubspec.yaml`, …), CI dirs, container files. All feature code lives under `src/`.

## Tech stack

**Not chosen yet.** The decision lives in `docs/adr/0001-tech-stack.md` (status: proposed). Task **P0-01** settles it. Replace this section with the real stack and versions once the ADR is `accepted`.

## Task tracking (agents: read this before starting any work)

The board is `docs/roadmap/README.md`. One file per phase. Every task is a **markdown checkbox** with a fixed shape:

```markdown
- [ ] **P2-03** — Log in as candidate or parent against the real API
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: —
      - Verify: a real candidate account reaches the point list
      - Notes: optional, one line
```

- **Start of a session:** read `docs/roadmap/README.md`, find the current phase, find the first unticked task whose `Needs` are all ticked. That is the next task.
- **Taking a task:** put the owner's name in, prefix the title with `🟡 DOING — `. Stuck → `🔴 BLOCKED — ` and say why in Notes.
- **Ticking a task:** run the **Verify** line first. It passes → `- [x]`, drop the marker, update the phase **Progress** line, update the count and phase box in `docs/roadmap/README.md`. All in one change.
- **Never** tick on "code written". Verify or it is not done.
- **Never** renumber or reuse an ID. New task → next free number, and raise the phase total.
- Branch = task ID: `P2-03-candidate-login`. Commit subject: `feat(P2-03): add candidate login`.
- Bugs are not roadmap tasks. They go to `docs/reference/bug-log.md` as `BUG-nnn`, and close only at `re-tested`.

## How we work — empirical development

Full rules: `docs/reference/working-rules.md`. Read it before starting work. The short form:

- **Decide from evidence, not assumption.** No "done", "fixed" or "works" without stating what you ran and what came out.
- **Loop:** transparency → inspection → adaptation. Make work visible, look at what is real, change the work or the plan.
- **Small batches.** One task = one change, ~200 lines. Merge to main daily. Main always works.
- **Contract first.** Agree the API before building the screen. A contract change needs both teams.
- **Verify or it is not done.** Real device, real network, real server. For anything that opens a locker, the server log is the evidence — not the screen.
- **Debug by hypothesis.** Reproduce → three possible causes → change one thing → verify on the device that found it. Changing code before you can explain the failure is gambling.
- **Assumptions get logged**, in the assumption log at the bottom of `working-rules.md`, with a date and a way to check them.

## Product principles

### File size (hard cap)
- No source file over **300 lines** (imports and blank lines count).
- Over the cap → split into more files in the **same feature folder** (`api`, `core`, `utils`). Never hide size with long lines or string tricks.
- Exceptions: pure config maps, large static tables, route registries.

### Feature colocation & nesting (strict)
- One macro-capability → one parent folder. Sub-capabilities → nested sub-folders.
- More than 5–7 files in one folder → extract a nested sub-folder.
- Code, types, tests and local docs of the same sub-domain stay together.
- Never dump domain logic into a global `utils/` or `types/` folder.

```text
  [WRONG - flat]            [CORRECT - nested]
  src/                      src/
  ├── auth_login/           └── auth/
  ├── auth_scan/                ├── login/
  └── auth_session/             ├── scan/
                                └── session/
```

### File placement & lifecycle

| Kind of file | Goes to | Lifecycle |
| --- | --- | --- |
| Shippable code | `src/<feature>/…` | — |
| Test | `tests/` mirroring `src/` | — |
| How a thing works now | `docs/reference/<topic>.md` | edit in place |
| Dated audit / test report | `docs/findings/YYYY-MM-DD-<topic>.md` | immutable; supersede → `docs/legacy/` |
| Non-obvious decision | `docs/adr/NNNN-title.md` | immutable; reverse with a new ADR |
| Cross-session lesson | memory (`MEMORY.md` row + one-fact file) | update; delete if wrong |
| Throwaway / probe | `scratch_*` (gitignored) | promote or delete before "done" |
| Generated artifact | gitignored data dir | never committed |

Each `docs/` bucket gets a one-line `README.md`. `docs/README.md` is the single index.

### Workspace hygiene (before "done")
1. No new root-level files outside the Repository map.
2. No `_copy`, `_old`, `temp`, or duplicate scripts.
3. Every new path is referenced by code, tests, or docs in the same change.
4. New top-level folder → add a row to the Repository map in the same change.
5. No `scratch_*` left in `src/` or committed.

### Findings & reports
- Prefer the smallest durable home first: update a `reference/` doc, or write an ADR. Write a dated finding only when the snapshot itself has value (e.g. a device test round).
- Line 1: `Parent: <relative-path>` or `Parent: none`. Sections: **Status**, **Scope**, **Evidence** (commands + outcomes), **Next** (numbered).
- Same topic, newer date → new file; `git mv` the old one to `docs/legacy/`; update `docs/README.md`.

## Verification

| Check | Command / rule |
| --- | --- |
| Tests | Fill in once the stack is chosen |
| Lint / Typecheck | Use project-standard commands; do not invent tooling |
| Behavior | State expected output; give a manual repro if no test exists |
| UI | Screenshot on a real Android and iOS device, compare to the agreed design |

Verification fails → fix it, or report the failing command and the error text. Never claim "done" on assumption.

## Engineering standards

- **Secrets:** never paste keys, tokens, cookies, or private URLs. Confirm `.gitignore` covers `.env` and `.claude/settings.local.json` before any commit.
- **Dependencies:** prefer the existing stack; justify every new package.
- **Real backend:** the app talks to the real Server-team API. Mocks are for tests only, and must be labelled.
- **Locker safety:** an "open locker" call is a physical action. Never fire it from retry loops or without explicit user intent.

## Agent orchestration

- Loop: `Discuss → Plan → Execute → Verify`.
- Plan first for any multi-step task; write the plan down before code.
- Max ~4 concurrent sub-agents. Same folder → run them one at a time.
- Reality drifts from the plan → update the plan if the pivot is safe; halt and ask if it is risky.
- After tasks are confirmed with the user, reply **AYE** once that turn.
- A sub-agent failing the same way 3 times → stop, record the state, escalate with a clear blocker.

## Git and delivery

- Commit every turn.
- Push / PR only when the user asks.
- Conventional commits (`feat:`, `fix:`, `refactor:`). One logical change per commit; the subject says *why*.
- Never commit secrets, `.env`, or large generated files.

## Maintaining this file

Add a rule when an agent makes the same mistake twice. Remove stale map rows when folders go. Past 200 lines → move file-type rules to `.claude/rules/<topic>.md`.
