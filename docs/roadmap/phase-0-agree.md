# Phase 0 — Agree

**Goal:** every unknown that would force a rebuild later is settled and written down.

**No app code in this phase.** Nothing here needs a tech stack, so nothing here is wasted work.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P0-01 | Pick the app tech: one code base for both phones, or two native apps. Write [ADR 0001](../adr/0001-tech-stack.md) | | todo | — | Blocks all app work |
| P0-02 | Get one real VGU student card. Scan it. Record the barcode type and the exact digits | | todo | — | Get a lecturer card too if the format differs |
| P0-03 | Get one real code from the VGU Library app. Scan it. Record its format | | todo | — | Ask the library team who owns that code |
| P0-04 | Confirm with the Server team: what links a scanned code to a user account | | todo | P0-02, P0-03 | Their table maps card code → user |
| P0-05 | Agree the API contract with the Server team. Fill in [api-contract.md](../reference/api-contract.md) | | todo | P0-04 | Both teams sign it. Highest-cost item |
| P0-06 | Agree the login rules for both account types: student/lecturer, and candidate/parent | | todo | P0-05 | Who gets which screens |
| P0-07 | Agree how a candidate or parent receives their password by email, and what the app shows first | | todo | P0-06 | Server sends the mail; app must match the wording |
| P0-08 | List the test devices. At least one real Android phone and one real iPhone. Record the OS versions | | todo | — | An emulator is not enough for camera or network tests |
| P0-09 | Set up the repo: branches, review rules, who merges | | todo | P0-01 | Branch name = task ID |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P0-01 | ADR 0001 status is `accepted` and names one tech |
| P0-02 | The barcode type and a real sample value are written in this repo |
| P0-03 | The library code format and a real sample value are written in this repo |
| P0-04 | The Server team confirmed the mapping in writing |
| P0-05 | `api-contract.md` has no `TO AGREE` markers left, and both teams have agreed to it |
| P0-06 | The rules are in `api-contract.md` under Account types |
| P0-07 | The email wording and the app's first screen for a new candidate are written down |
| P0-08 | The device list is in this file, with model and OS version |
| P0-09 | A second person can clone the repo and open a branch by following the README |

## Exit check

Phase 0 ends when all nine rows are `done`. Then update the count in [README.md](README.md) and move to Phase 1.

## Devices

Fill this in for P0-08.

| Device | OS version | Owner |
| --- | --- | --- |
| | | |
