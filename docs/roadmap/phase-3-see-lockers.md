# Phase 3 — See the lockers

**Goal:** the user finds a locker point with free space, near them, and sees what is inside it.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P3-01 | Show the list of locker points from the API | | todo | P2-02 | |
| P3-02 | Show the free-slot count for each point | | todo | P3-01 | Comes from the server. The app never counts it itself |
| P3-03 | Ask for location permission, and work fine if the user says no | | todo | P3-01 | "No" must not block the app. Show the plain list |
| P3-04 | Sort or mark the nearest point | | todo | P3-03 | Confirm with the Server team: does the API send the point's location? |
| P3-05 | Show the locker list inside one point, with each locker's state | | todo | P3-01 | |
| P3-06 | Show each locker's size | | todo | P3-05 | Sizes come from the server table |
| P3-07 | Refresh the list. Free counts go stale fast | | todo | P3-02 | Pull to refresh, plus refresh when the screen opens |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P3-01 | Every point on the server appears in the app |
| P3-02 | The count in the app matches the server after a refresh |
| P3-03 | Denying location leaves the app fully usable |
| P3-04 | Standing at one point, that point is first or marked |
| P3-05 | Locker states in the app match the server |
| P3-06 | Each locker shows its size |
| P3-07 | A change made on the server appears in the app after one refresh |

## Exit check

A user with no prior knowledge can open the app, find the nearest point with free space, and see the lockers in it. Then update the count in [README.md](README.md).
