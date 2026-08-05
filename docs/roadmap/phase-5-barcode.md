# Phase 5 — Barcode

**Goal:** the user opens a locker by scanning their card, and still gets in when the scan fails.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P5-01 | Build the camera scan screen | | todo | P4-02 | |
| P5-02 | Ask for camera permission, and explain why before asking | | todo | P5-01 | Denied → send the user to the fallback, not to a dead end |
| P5-03 | Read the student and lecturer card barcode | | todo | P5-01, P0-02 | Use the real format from P0-02 |
| P5-04 | Read the code from the VGU Library app | | todo | P5-01, P0-03 | Same screen if the format allows it |
| P5-05 | Send the scanned code to the API and open the matching locker | | todo | P5-03, P4-02 | The server does the matching. The app only sends the code |
| P5-06 | Fallback: open with ID and password when the scan fails | | todo | P5-05 | Always reachable from the scan screen |
| P5-07 | Handle a bad scan: dirty card, dark room, unknown code | | todo | P5-03 | Plain message, offer the fallback, never crash |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P5-01 | The camera view opens on both phones |
| P5-02 | Denying the camera still leads to a working fallback |
| P5-03 | A real student card scans on the first or second try |
| P5-04 | A real library-app code scans |
| P5-05 | Scanning a real card opens the real locker |
| P5-06 | With the camera covered, a user still opens their locker |
| P5-07 | A random unknown barcode shows a plain message and no crash |

## Exit check

A real card opens a real locker on both phones. The fallback works with the camera blocked. Then update the count in [README.md](README.md).

## Note

Do not start P5-03 before P0-02 is `done`. Building against a guessed barcode format means building it twice.
