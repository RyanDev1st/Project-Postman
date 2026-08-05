# Phase 0 — Agree

**Goal:** every unknown that would force a rebuild later is settled and written down.

**Progress: 0 / 9.** Status: **CURRENT**

**No app code in this phase.** Nothing here needs a tech stack, so nothing here can be wasted.

## Tasks

- [ ] **P0-01** — Pick the app tech: one code base for both phones, or two native apps
      - Owner: _unassigned_ · Needs: — · Blocks: P0-09, P1-01, P1-02, P1-03
      - Verify: [ADR 0001](../adr/0001-tech-stack.md) status is `accepted` and names one tech
      - Notes: blocks every app task in the repo. Read the ADR's "What to check" list first

- [ ] **P0-02** — Get one real VGU student card. Scan it. Record the barcode type and the exact digits
      - Owner: _unassigned_ · Needs: — · Blocks: P0-04, P5-03
      - Verify: the barcode type and a real sample value are written in this repo
      - Notes: get a lecturer card too. Confirm whether the format differs

- [ ] **P0-03** — Get one real code from the VGU Library app. Scan it. Record its format
      - Owner: _unassigned_ · Needs: — · Blocks: P0-04, P5-04
      - Verify: the format and a real sample value are written in this repo
      - Notes: ask the library team who owns that code

- [ ] **P0-04** — Confirm with the Server team what links a scanned code to a user account
      - Owner: _unassigned_ · Needs: P0-02, P0-03 · Blocks: P0-05
      - Verify: the Server team confirmed the mapping in writing
      - Notes: their table maps card code → user. The app never does this matching

- [ ] **P0-05** — Agree the API contract with the Server team
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P0-06, P1-04, P7-01
      - Verify: [api-contract.md](../reference/api-contract.md) has no `TO AGREE` markers left, and both teams agreed to it
      - Notes: highest-cost item in the project. Change it late and the screens get rebuilt

- [ ] **P0-06** — Agree the login rules for both account types
      - Owner: _unassigned_ · Needs: P0-05 · Blocks: P0-07, P2-02, P2-03
      - Verify: the rules are written in `api-contract.md` under **Account types**
      - Notes: student/lecturer vs candidate/parent. Who sees which screens

- [ ] **P0-07** — Agree how a candidate or parent receives their password by email, and what the app shows them first
      - Owner: _unassigned_ · Needs: P0-06 · Blocks: P2-03
      - Verify: the email wording and the app's first screen for a new candidate are written down
      - Notes: the server sends the mail. The app's words must match it

- [ ] **P0-08** — List the test devices: at least one real Android phone and one real iPhone
      - Owner: _unassigned_ · Needs: — · Blocks: P1-02, P1-03
      - Verify: the Devices table below is filled in, with model and OS version
      - Notes: an emulator is not enough for camera, network or hardware tests

- [ ] **P0-09** — Set up the repo: branches, review rules, who merges
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: P1-01
      - Verify: a second person can clone the repo and open a branch by following the README
      - Notes: branch name = task ID

## Devices

Fill this in for P0-08.

| Device | Model | OS version | Owner |
| --- | --- | --- | --- |
| Android | | | |
| iPhone | | | |

## Exit check

- [ ] All nine tasks ticked
- [ ] `api-contract.md` has zero `TO AGREE` markers
- [ ] ADR 0001 is `accepted`
- [ ] Counts updated in [README.md](README.md)

Then move to Phase 1.
