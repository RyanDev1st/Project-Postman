# Phase 7 — Parcel handoff

**Goal:** a parcel is dropped in a locker and the receiver gets a code to open it.

**Marked *future* in the team requirements. Do it last.** Start only when Phases 1–6 are `done` and the Server team has the order API ready.

| ID | Task | Owner | Status | Depends on | Notes |
| --- | --- | --- | --- | --- | --- |
| P7-01 | Agree the order data with the Server team: order code, locker, user, send time | | todo | P0-05 | Extend the API contract first, as in Phase 0 |
| P7-02 | Read the scanned locker or order data | | todo | P7-01, P5-05 | |
| P7-03 | Ask the API for a suitable free locker, by parcel size | | todo | P7-02, P3-06 | The **server** picks the locker. The app sends the size and asks |
| P7-04 | Confirm the drop and record the order | | todo | P7-03 | |
| P7-05 | Trigger the open code being sent to the receiver | | todo | P7-04 | The server sends it. Confirm the channel: email, SMS, or in-app |
| P7-06 | The receiver opens the locker with that code | | todo | P7-05, P4-02 | Handle an expired or used code with a plain message |

## Verify

| ID | This task is `done` when |
| --- | --- |
| P7-01 | The order fields are in `api-contract.md` and both teams agreed |
| P7-02 | Scanned order data appears correctly in the app |
| P7-03 | A large parcel never gets a small locker |
| P7-04 | The order row exists on the server with the right locker and time |
| P7-05 | The receiver actually receives the code |
| P7-06 | The code opens the real locker once, and fails cleanly the second time |

## Exit check

One parcel goes end to end: dropped by a sender, collected by a different person with a code. Then update the count in [README.md](README.md).

## Warning

This phase gives one person a code to open a locker holding another person's parcel. Every rule from [Phase 4](phase-4-open-locker.md) applies, plus: a code works **once**, and a code expires.
