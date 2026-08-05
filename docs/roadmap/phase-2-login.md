# Phase 2 — Login

**Goal:** both account types log in, stay logged in, and see a clear message when something fails.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P2-01 | Build the login screen UI | | todo | P1-07 | |
| P2-02 | Log in as student or lecturer against the real API | | todo | P2-01, P1-04 | |
| P2-03 | Log in as candidate or parent against the real API | | todo | P2-02 | These accounts come by email from the server |
| P2-04 | Store the pass token safely on the phone | | todo | P2-02 | Use the phone's secure store. Never plain text, never a log |
| P2-05 | Stay logged in after the app closes and reopens | | todo | P2-04 | |
| P2-06 | Send the user back to login when the token expires | | todo | P2-04 | The server answers "not allowed". Do not loop |
| P2-07 | Clear message on wrong password. No technical words | | todo | P2-02 | Never say which part was wrong |
| P2-08 | Clear message on no network, on every screen | | todo | P2-02 | Airplane mode must never crash the app |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P2-01 | The screen matches the agreed design on both phones |
| P2-02 | A real student account reaches the point list |
| P2-03 | A real candidate account reaches the point list |
| P2-04 | The token is not found in app logs or in plain app storage |
| P2-05 | Force-close, reopen → still logged in, no password asked |
| P2-06 | An expired token sends the user to login, once, with a message |
| P2-07 | A wrong password shows a plain message, and the app stays usable |
| P2-08 | Airplane mode on every screen shows a message, no crash, no blank screen |

## Exit check

Two real accounts, one of each type, log in on both phones. Airplane mode is handled everywhere. Then update the count in [README.md](README.md).

## Note on account types

The two types see different things. Confirm the exact difference in [api-contract.md](../reference/api-contract.md) under **Account types** before building P2-03.
