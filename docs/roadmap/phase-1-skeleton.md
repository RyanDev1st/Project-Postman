# Phase 1 — Skeleton

**Goal:** an empty app runs on both real phones and reaches the real server. No features yet.

This phase proves the pipe works. Every later phase pushes data through this pipe.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P1-01 | Create the app project in the chosen tech. Commit it | | todo | P0-01 | Follow the folder rules in `CLAUDE.md` |
| P1-02 | Build and run the empty app on a real Android phone | | todo | P1-01, P0-08 | |
| P1-03 | Build and run the empty app on a real iPhone | | todo | P1-01, P0-08 | Needs an Apple developer account. Start this early |
| P1-04 | Write the one place in the app that talks to the server. All requests go through it | | todo | P0-05 | One file. Later phases only add calls to it |
| P1-05 | Call one test endpoint on the real server and show the answer on screen | | todo | P1-04 | Proves the pipe end to end |
| P1-06 | Move the server address out of the code into config. One for test, one for real | | todo | P1-05 | Never a hard-coded address, never a key in the code |
| P1-07 | Wire the empty screens together: login → point list → locker list → locker | | todo | P1-01 | Empty screens. Navigation only |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P1-01 | `git clone` then one build command produces an app |
| P1-02 | The app is installed on the Android device from P0-08 and opens |
| P1-03 | The app is installed on the iPhone from P0-08 and opens |
| P1-04 | Every network call in the repo goes through this one file. Grep proves it |
| P1-05 | The real server's answer is visible on the phone screen |
| P1-06 | Switching test ↔ real needs a config change only, no code edit |
| P1-07 | A person can walk all four screens forward and back with no crash |

## Exit check

Both phones run the app. The app shows a real answer from the real server. Then update the count in [README.md](README.md).
