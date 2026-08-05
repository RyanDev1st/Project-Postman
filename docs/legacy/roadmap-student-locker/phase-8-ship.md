# Phase 8 — Harden and ship

**Goal:** the app survives real phones, real networks and real users, and is installed from a signed release build.

**Progress: 0 / 9.**

Device testing starts at the end of **every** phase, not only here. This phase is the final sweep.

## Tasks

- [ ] **P8-01** — Walk every screen on a real Android phone. Log every bug
      - Owner: _unassigned_ · Needs: P6-05 · Blocks: P8-03, P8-04, P8-07
      - Verify: every screen was opened on the real Android device, and the result is in the bug log
      - Notes: log to [bug-log.md](../reference/bug-log.md) as you go, not afterwards

- [ ] **P8-02** — Walk every screen on a real iPhone. Log every bug
      - Owner: _unassigned_ · Needs: P6-05 · Blocks: P8-03
      - Verify: every screen was opened on the real iPhone, and the result is in the bug log

- [ ] **P8-03** — Fix every logged bug, then test it again on the device that found it
      - Owner: _unassigned_ · Needs: P8-01, P8-02 · Blocks: P8-08
      - Verify: every bug row is `fixed` **and** `re-tested`
      - Notes: a fix is not done until it is re-tested on the same device

- [ ] **P8-04** — Airplane-mode test on every screen
      - Owner: _unassigned_ · Needs: P8-01, P2-08 · Blocks: P8-05
      - Verify: airplane mode on each screen shows a plain message. No crash, no blank screen, no false success

- [ ] **P8-05** — Slow-network test on every screen
      - Owner: _unassigned_ · Needs: P8-04 · Blocks: —
      - Verify: a 5-second-delay network never leaves the user with no feedback
      - Notes: slow is worse than off. The app must not hang forever

- [ ] **P8-06** — Check that no key, token, password or private address sits in the app code
      - Owner: _unassigned_ · Needs: P1-06 · Blocks: P8-08
      - Verify: a grep of the repo for keys and tokens returns nothing
      - Notes: anything found → rotate it first, then remove it. Removing it alone is not enough

- [ ] **P8-07** — Small-screen and large-text test
      - Owner: _unassigned_ · Needs: P8-01 · Blocks: —
      - Verify: at the largest system text size, every button is still reachable

- [ ] **P8-08** — Build the release version and install it on a clean phone
      - Owner: _unassigned_ · Needs: P8-03, P8-06 · Blocks: P8-09
      - Verify: the release build installs and runs on a phone that never had the app
      - Notes: a clean phone finds bugs a developer phone hides

- [ ] **P8-09** — Write the install and rollback steps for whoever runs the release
      - Owner: _unassigned_ · Needs: P8-08 · Blocks: —
      - Verify: a person who did not build the app can follow the page and release it
      - Notes: one page. Include what to do if the release turns out bad

## Exit check

- [ ] All nine tasks ticked
- [ ] The release build runs on a clean Android **and** a clean iPhone
- [ ] The bug log has zero `open` rows
- [ ] Counts updated in [README.md](README.md)
