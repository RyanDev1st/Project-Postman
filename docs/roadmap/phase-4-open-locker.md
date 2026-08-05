# Phase 4 — Open the locker

**Goal:** the user opens and locks a locker, and can see what they did before.

**This is the core phase.** An "open" request moves real metal. Treat every task here as safety work.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P4-01 | Build the locker screen with an open button and a lock button | | todo | P3-05 | |
| P4-02 | Send the open request to the API | | todo | P4-01, P1-04 | |
| P4-03 | Send the lock request to the API | | todo | P4-02 | |
| P4-04 | Block double taps and repeat sends. One tap = at most one open | | todo | P4-02 | **Never auto-retry an open request** |
| P4-05 | Show clear success and clear failure states, and what to do next | | todo | P4-02 | "Locker 12 is open" beats a spinner that stops |
| P4-06 | Handle the network dropping in the middle of an open request | | todo | P4-04 | The app does not know if it opened. Say so, and tell the user to check |
| P4-07 | Show my own open and lock history from the API | | todo | P2-02 | |
| P4-08 | Show a plain message when the server refuses: not my locker, wrong mode, locker faulty | | todo | P4-02 | List every refusal reason in the API contract |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P4-01 | Both buttons appear and are reachable with one thumb |
| P4-02 | The real locker opens |
| P4-03 | The real locker locks |
| P4-04 | Ten fast taps produce exactly one open request in the server log |
| P4-05 | A tester with no training knows if it worked, without asking |
| P4-06 | Killing the network mid-request never opens a second time and never shows a false success |
| P4-07 | The history in the app matches the server's history rows |
| P4-08 | Each refusal reason shows its own plain message. No error codes on screen |

## Exit check

A person opens and locks a real locker on both phones. P4-04 and P4-06 are proven with the server log, not by assumption. Then update the count in [README.md](README.md).

## Safety rules for this phase

1. Never fire an open request from a retry loop, a timer, or a screen refresh.
2. Never fire an open request without a direct user tap.
3. If the answer is unclear, say it is unclear. Never guess "success".
