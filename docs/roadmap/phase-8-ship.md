# Phase 8 — Harden and ship

**Goal:** the system survives real phones, real networks and real people, and installs from a signed release build.

**Progress: 0 / 10.**

Device testing starts at the end of **every** phase, not only here. This phase is the final sweep.

## Tasks

- [ ] **P8-01** — Walk every screen on a real Android phone. Log every bug
      - Owner: _unassigned_ · Needs: P7-03 · Blocks: P8-04, P8-05, P8-08
      - Verify: every screen was opened on the real Android device, and the result is in the bug log
      - Notes: log to [bug-log.md](../reference/bug-log.md) as you go, not afterwards

- [ ] **P8-02** — Walk every screen on a real iPhone. Log every bug
      - Owner: _unassigned_ · Needs: P7-03 · Blocks: P8-04
      - Verify: every screen was opened on the real iPhone, and the result is in the bug log

- [ ] **P8-03** — Walk every screen on the real cabinet. Log every bug
      - Owner: _unassigned_ · Needs: P7-03 · Blocks: P8-04
      - Verify: every cabinet screen was used at the cabinet, and the result is in the bug log

- [ ] **P8-04** — Fix every logged bug, then test it again on the device that found it
      - Owner: _unassigned_ · Needs: P8-01, P8-02, P8-03 · Blocks: P8-09
      - Verify: every bug row is `fixed` **and** `re-tested`
      - Notes: a fix is not done until it is re-tested on the same device

- [ ] **P8-05** — Airplane-mode test on every screen
      - Owner: _unassigned_ · Needs: P8-01 · Blocks: P8-06
      - Verify: airplane mode on each screen shows a plain message. No crash, no blank screen, no false success

- [ ] **P8-06** — Slow-network test on every screen
      - Owner: _unassigned_ · Needs: P8-05 · Blocks: —
      - Verify: a 5-second-delay network never leaves the user with no feedback
      - Notes: slow is worse than off. Nothing may hang forever

- [ ] **P8-07** — Check that no key, token or address sits in either build
      - Owner: _unassigned_ · Needs: P1-07 · Blocks: P8-09
      - Verify: a search of both source trees for keys and addresses returns nothing
      - Notes: anything found → replace it first, then remove it. Removing it alone is not enough

- [ ] **P8-08** — Small-screen and large-text test on the app
      - Owner: _unassigned_ · Needs: P8-01 · Blocks: —
      - Verify: at the largest system text size, every button is still reachable

- [ ] **P8-09** — Build the release version and install it on a clean phone
      - Owner: _unassigned_ · Needs: P8-04, P8-07 · Blocks: P8-10
      - Verify: the release build installs and runs on a phone that never had the app
      - Notes: a clean phone finds bugs a developer phone hides

- [ ] **P8-10** — Write the install and rollback steps for whoever runs the release
      - Owner: _unassigned_ · Needs: P8-09 · Blocks: —
      - Verify: a person who did not build the app can follow the page and release it
      - Notes: one page. Include what to do if the release turns out bad, and how to reach a stuck cabinet

## Exit check

- [ ] All ten tasks ticked
- [ ] The release build runs on a clean Android **and** a clean iPhone
- [ ] The cabinet screen runs from a fresh install
- [ ] The bug log has zero `open` rows
- [ ] Counts updated in [README.md](README.md)
