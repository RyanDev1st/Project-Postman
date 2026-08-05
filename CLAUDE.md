# Project Postman — VGU Smart Locker — Agent Workspace

Rules for Claude Code and other agents. This file loads every session. Keep it **under 200 lines**.

## Mission

Build a smart locker system for VGU that works. A person opens a locker with a phone. Staff manage the locker points, the modes and the history.

This repo holds the **IT team work — the app**. The Server team owns the API and the database. We call their API. We do not change it.

| In scope | Out of scope (unless the user says otherwise) |
| --- | --- |
| The phone app (Android and iOS), screens, login, open and lock a locker, barcode scan, no-network handling | Server code, database design, locker hardware |
| Work against the real Server-team API | Fake servers in shipped code. Test fakes are allowed, and carry a label |
| | Secrets in the repo, in a commit, or in chat |

## Feature list (from the team requirements)

The app (this repo):
1. Screens: login, locker-point list, locker list, open and lock a locker.
2. Login for 2 account types: student or lecturer, and candidate or parent.
3. Call the Server API. Handle no network. Handle a wrong password.
4. Test on real Android and iOS phones. Write down every bug, then fix it.
5. Show each locker point with its free-slot count. Suggest the nearest point.
6. Scan the card barcode of a student or lecturer to open a locker. The code in the VGU Library app also works. If the scan fails, the user logs in with ID and password.
7. Later: parcel handoff. Read the scanned locker data, pick a free locker, then send the open code to the receiver.

The Server team (not this repo, but we depend on it):
- Tables: users, locker, open and lock history, school events.
- API: log in, issue a password, open a locker, read the usage history.
- Send the login name and password by email to a candidate or parent, without staff action.
- Back up on a schedule. Set access rights per user group.
- Locker state per mode: exam-reserved or shared. Locker size. Order data.
- Each card barcode maps to one user account.
- Many locker points. Each point has its own code, locker list, state and mode.
- Two modes: normal and exam season. An admin switches between them.

## Repository map

| Path | Purpose |
| --- | --- |
| `CLAUDE.md` | Team agent instructions (this file) |
| `README.md` | For people. Status, and where things are |
| `docs/` | Documentation. The index is `docs/README.md` |
| `docs/roadmap/` | The plan and the task board. One file per phase. Task IDs are `P<phase>-<nn>` |
| `docs/reference/` | How the system works **now** — architecture, API contract, designs. Edit these in place |
| `docs/findings/` | Dated audits and test reports. Never edit one. A newer date replaces it |
| `docs/legacy/` | Docs that are no longer true. Move them here. Never delete them |
| `docs/adr/` | One short file per hard decision. It holds the *why* |
| `src/` | App source code. Feature folders only |
| `tests/` | Tests that mirror `src/` |
| `scripts/` | Build and automation scripts |
| `.claude/` | Agent context, skills, and rules for named file types |

**Root policy:** the repo root holds only these — `CLAUDE.md`, `.gitignore`, setup files such as `package.json` or `pubspec.yaml`, CI folders, and container files. All feature code goes under `src/`.

## Tech stack

**Not chosen yet.** The decision sits in `docs/adr/0001-tech-stack.md`, status `proposed`. Task **P0-01** decides it. When that ADR is `accepted`, replace this section with the real stack and the version numbers.

## Task tracking (agents: read this before you start any work)

The board is `docs/roadmap/README.md`. One file per phase. Every task is a checkbox. The shape never changes:

```markdown
- [ ] **P2-03** — Log in as candidate or parent against the real API
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: —
      - Verify: a real candidate account reaches the point list
      - Notes: optional, one line
```

- **At the start of a session:** open `docs/roadmap/README.md`. Find the current phase. Find the first unticked task where every `Needs` task is ticked. That is the next task.
- **To take a task:** write your name in `Owner`. Put `🟡 DOING — ` in front of the title. If you get stuck, change it to `🔴 BLOCKED — ` and write why in `Notes`.
- **To tick a task:** run the `Verify` line first. If it passes, change `- [ ]` to `- [x]`. Remove the marker. Update the phase `Progress` line. Update the count and the phase box in `docs/roadmap/README.md`. Do all of this in one change.
- **Never** tick a task because the code is written. If `Verify` did not pass, it is not done.
- **Never** renumber an ID. **Never** reuse one. A new task takes the next free number. Raise the phase total in the same change.
- Name the branch after the task ID: `P2-03-candidate-login`. Write the commit subject as `feat(P2-03): add candidate login`.
- A bug is not a roadmap task. Bugs go to `docs/reference/bug-log.md` as `BUG-nnn`. A bug closes only when it is `re-tested`.

## How we work — empirical development

**`docs/reference/working-rules.md` is the authority.** What follows is a short summary for agents. If the two disagree, the doc wins, and you fix this summary in the same change. Never change a rule in this file alone.

The short form:

- **Decide from evidence, not from assumption.** Never say "done", "fixed" or "works" without saying what you ran and what came out.
- **Work in a loop:** show the work, look at what is real, then change the work or the plan.
- **Keep changes small.** One task is one change, about 200 lines. Merge to main every day. Main always works.
- **Agree the contract first.** Agree the API before you build the screen. A contract change needs both teams.
- **Check it, or it is not done.** Real phone, real network, real server. When a locker opens, the server log is the evidence — not the screen.
- **Debug by possible cause.** Reproduce the bug. List three possible causes. Change one thing. Check on the phone that found it. If you change code before you can explain the failure, you are guessing.
- **Write down every assumption** in the assumption log at the end of `working-rules.md`, with a date and a way to check it.
- **Say your assumptions before you build.** Two ways to read the task? Show both and ask. Never pick one in silence.
- **Build the simplest thing that solves it.** No feature, setting or wrapper that nobody asked for. 200 lines that could be 50? Rewrite them.
- **Change only what the task needs.** Do not tidy nearby code. Match the style that is already there. Remove what *your* change left unused. Dead code you found: say so, and leave it.

Those last three are group **J** in `working-rules.md`. The full text is in the installed skill `andrej-karpathy-skills:karpathy-guidelines`. Load that skill. Never search the web for it.

## Product principles

### File size (hard cap)
- No source file goes over **300 lines**. Imports and blank lines count.
- Over the cap? Split it into more files in the **same feature folder**, such as `api`, `core` or `utils`. Never hide the size with long lines or joined strings.
- Three exceptions: config maps, large fixed tables, route lists.

### Keep a feature together (strict)
- One big feature gets one parent folder. Each smaller part gets a folder inside it.
- More than 5 to 7 files in one folder? Move some of them into a folder inside it.
- Code, types, tests and local docs for the same part stay together.
- Never put feature code in a shared `utils/` or `types/` folder.

```text
  [WRONG - flat]            [CORRECT - nested]
  src/                      src/
  ├── auth_login/           └── auth/
  ├── auth_scan/                ├── login/
  └── auth_session/             ├── scan/
                                └── session/
```

### Where a file goes, and how long it lives

| Kind of file | Goes to | How long it lives |
| --- | --- | --- |
| Code we ship | `src/<feature>/…` | — |
| Test | `tests/`, mirroring `src/` | — |
| How a thing works now | `docs/reference/<topic>.md` | Edit it in place |
| Dated audit or test report | `docs/findings/YYYY-MM-DD-<topic>.md` | Never edit it. A newer file replaces it. Move the old one to `docs/legacy/` |
| A hard decision | `docs/adr/NNNN-title.md` | Never edit it. To reverse it, write a new ADR |
| A lesson for later sessions | Memory: a `MEMORY.md` row and a one-fact file | Update it. Delete it if it turns out wrong |
| Throwaway or probe | `scratch_*`, which git ignores | Before you say done, keep it properly or delete it |
| Generated file | A data folder that git ignores | Never commit it |

Each folder in `docs/` gets a one-line `README.md`. `docs/README.md` is the one index.

### Tidy up before you say done
1. No new file at the repo root unless the Repository map lists it.
2. No `_copy`, `_old` or `temp` files. No duplicate scripts.
3. Code, tests or docs point at every new file, in the same change.
4. A new top-level folder needs a new row in the Repository map, in the same change.
5. No `scratch_*` file is left in `src/`, and none is committed.

### Findings and reports
- Put it in the smallest place that lasts. Update a `reference/` doc, or write an ADR. Write a dated finding only when the snapshot itself is worth keeping, such as a round of device tests.
- Line 1 is `Parent: <relative-path>` or `Parent: none`. Then these sections: **Status**, **Scope**, **Evidence** (the commands and what came out), **Next** (a numbered list).
- Same topic on a later date? Write a new file. Move the old one to `docs/legacy/` with `git mv`. Update `docs/README.md`.

## How we check work

| Check | Command or rule |
| --- | --- |
| Tests | Fill this in once the stack is chosen |
| Lint and types | Use the project's own commands. Do not invent new tools |
| Behavior | Say what output you expect. If there is no test, give the steps to reproduce |
| Screens | Take a screenshot on a real Android phone and a real iPhone. Compare to the agreed design |

If a check fails, fix it. If you cannot fix it, report the command that failed and the error text. Never say done because you believe it works.

## Engineering standards

- **Secrets:** never paste a key, token, cookie or private address. Before any commit, check that `.gitignore` covers `.env` and `.claude/settings.local.json`.
- **Packages:** use what the project already has. Give a reason for every new package.
- **Real server:** the app talks to the real Server-team API. Fakes belong in tests only, and carry a label.
- **Locker safety:** an open-locker call moves real metal. Send it only when the user taps. Never send it from a retry loop.

## How agents run work

- Work in this order: discuss, plan, build, check.
- Any task with more than one step gets a plan. Write the plan down before you write code.
- Run at most 4 sub-agents at once. If two of them would write to the same folder, run them one at a time.
- If the real work moves away from the plan, update the plan when the change is safe. If the change is risky, stop and ask.
- When the user confirms the tasks, reply **AYE** once in that turn.
- If a sub-agent fails the same way three times, stop. Write down where it got to. Tell the user what is blocking it.

## Git and delivery

- Commit every turn.
- Push, or open a pull request, only when the user asks.
- Use conventional commits: `feat:`, `fix:`, `refactor:`. One change per commit. The subject says **why**.
- Never commit a secret, a `.env` file, or a large generated file.

## Maintaining this file

Write this file in **STE — Simplified Technical English**. Short sentences. One idea per sentence. Plain words over technical ones. The same word always means the same thing. Keep new text in the same style.

Add a rule when an agent makes the same mistake twice. Remove a map row when its folder goes.

**Where a rule belongs** (decided in [ADR 0002](docs/adr/0002-where-rules-live.md)):

| Kind of rule | Home |
| --- | --- |
| How the team works. It needs the *why*, and people read it | `docs/reference/working-rules.md`. Summarize it here only if an agent needs it every session |
| Agent behavior needed on **every** task | This file |
| Agent behavior for **named file types**, such as screen files or API-call files | `.claude/rules/<topic>.md` with `paths:` at the top. **None yet** — `src/` is empty until P0-01 picks the stack |

`.claude/rules/` files load every session, the same as this file. The split makes them easier to read. It does not save context. Move file-type rules there when this file passes 200 lines.
