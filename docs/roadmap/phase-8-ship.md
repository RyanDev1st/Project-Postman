# Phase 8 — Harden and ship

**Goal:** the app survives real phones, real networks and real users, and is installed from a store or a signed build.

Device testing starts at the end of **every** phase, not only here. This phase is the final sweep.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P8-01 | Walk every screen on a real Android phone. Log every bug | | todo | P6-05 | Log to [bug-log.md](../reference/bug-log.md) |
| P8-02 | Walk every screen on a real iPhone. Log every bug | | todo | P6-05 | |
| P8-03 | Fix every logged bug, then test it again on the device that found it | | todo | P8-01, P8-02 | A fix is not done until it is re-tested |
| P8-04 | Airplane-mode test on every screen | | todo | P8-01 | No crash, no blank screen, no false success |
| P8-05 | Slow-network test on every screen | | todo | P8-04 | Slow is worse than off. The app must not hang forever |
| P8-06 | Check no key, token, password or private address sits in the app code | | todo | P1-06 | Grep the repo. Anything found → rotate it, then remove it |
| P8-07 | Small-screen and large-text test | | todo | P8-01 | Nothing may become unreachable |
| P8-08 | Build the release version and install it on a clean phone | | todo | P8-03 | A clean phone finds bugs a developer phone hides |
| P8-09 | Write the install and rollback steps for whoever runs the release | | todo | P8-08 | One page. What to do if the release is bad |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P8-01 | Every screen was opened on the real Android device, and the result is in the bug log |
| P8-02 | Same on the real iPhone |
| P8-03 | Every bug row is `fixed` **and** `re-tested` |
| P8-04 | Airplane mode on each screen shows a plain message. No crash |
| P8-05 | A 5-second-delay network never leaves the user with no feedback |
| P8-06 | A grep for keys and tokens returns nothing |
| P8-07 | At the largest system text size, every button is still reachable |
| P8-08 | The release build installs and runs on a phone that never had the app |
| P8-09 | A person who did not build the app can follow the page and release it |

## Exit check

Release build runs on a clean Android and a clean iPhone. Bug log has no open rows. Then update the count in [README.md](README.md).
