# Phase 6 — Modes

**Goal:** the app follows the school's two operating modes without a reinstall.

Two modes exist: **normal** and **exam season**. In exam season some lockers are reserved for candidates. The admin switches the mode on the server. The app only follows.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P6-01 | Read the current mode of each point from the API | | todo | P3-01 | The mode is per point, not one global mode |
| P6-02 | Show the point's mode on screen in plain words | | todo | P6-01 | "Exam season — some lockers are reserved" |
| P6-03 | Show reserved lockers correctly in exam mode, and say who they are for | | todo | P6-01, P3-05 | The server decides. The app never guesses |
| P6-04 | Follow a mode change made by the admin, with no app update | | todo | P6-01 | Next refresh picks it up |
| P6-05 | Refuse an open on a locker the current mode does not allow, with a plain reason | | todo | P6-03, P4-08 | The server refuses. The app explains |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P6-01 | The mode in the app matches the server for every point |
| P6-02 | A user reads the screen and knows which mode the point is in |
| P6-03 | In exam mode, reserved lockers look different from shared lockers |
| P6-04 | The admin flips the mode; the app shows the new mode after one refresh, no reinstall |
| P6-05 | Tapping open on a not-allowed locker gives a plain reason, not an error code |

## Exit check

The admin switches a point from normal to exam season. The app follows on both phones with no reinstall. Then update the count in [README.md](README.md).
