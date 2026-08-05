# API contract — app ↔ server

**Status: DRAFT. Not agreed.** Every `TO AGREE` marker is an open question for the Server team.

This is the single most expensive document in the project. If it changes after the screens are built, the screens are rebuilt. Settle it in Phase 0. See task P0-05.

## How to use this file

- The **app team** writes what the app needs.
- The **server team** writes what the server can give.
- Both teams read it together, remove every `TO AGREE`, and agree it.
- After that, a change to this file is a change both teams agree to first.

## Ground rules

| Rule | Value |
| --- | --- |
| Transport | HTTPS only. No plain HTTP, not even in test |
| Format | JSON |
| Auth | Token in the request header, after login |
| Time | One time format everywhere — `TO AGREE` (suggest UTC, ISO 8601) |
| Errors | Every failure has a stable code the app can switch on, plus a message the server does **not** expect the app to show raw |

## Account types

`TO AGREE` — fill in with the Server team. See task P0-06.

| Type | Who | Gets the account how | Sees what |
| --- | --- | --- | --- |
| Student / lecturer | Enrolled or employed | School identity | `TO AGREE` |
| Candidate / parent | Exam visitors | Login name + password by email from the server | `TO AGREE` |

## Endpoints the app needs

One row per call. Fill in the real path, request and answer during P0-05.

### Login and identity

| # | What the app wants | Path | Sends | Gets back | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | Log in with ID and password | `TO AGREE` | id, password | token, account type, expiry | `TO AGREE` |
| 2 | Refresh or check the token | `TO AGREE` | token | valid or not | `TO AGREE` |
| 3 | Log out | `TO AGREE` | token | ok | `TO AGREE` |

### Locker points and lockers

| # | What the app wants | Path | Sends | Gets back | Status |
| --- | --- | --- | --- | --- | --- |
| 4 | List all locker points | `TO AGREE` | token | point code, name, free count, mode, location? | `TO AGREE` |
| 5 | List lockers in one point | `TO AGREE` | token, point code | locker id, size, state, reserved-for | `TO AGREE` |

### Opening

| # | What the app wants | Path | Sends | Gets back | Status |
| --- | --- | --- | --- | --- | --- |
| 6 | Open a locker | `TO AGREE` | token, locker id | opened, or a refusal code | `TO AGREE` |
| 7 | Lock a locker | `TO AGREE` | token, locker id | locked, or a refusal code | `TO AGREE` |
| 8 | Open by scanned card code | `TO AGREE` | token?, scanned code | which locker opened, or a refusal code | `TO AGREE` |
| 9 | My open and lock history | `TO AGREE` | token | list of time, locker, action | `TO AGREE` |

### Parcel handoff — Phase 7, later

| # | What the app wants | Path | Sends | Gets back | Status |
| --- | --- | --- | --- | --- | --- |
| 10 | Ask for a free locker by parcel size | `TO AGREE` | token, point code, size | locker id | `TO AGREE` |
| 11 | Record the drop and send the code to the receiver | `TO AGREE` | token, locker id, receiver | order code | `TO AGREE` |
| 12 | Open with a one-time parcel code | `TO AGREE` | order code | opened, or a refusal code | `TO AGREE` |

## Refusal reasons

The app must show a different plain message for each one. List every reason the server can send. `TO AGREE`.

| Code | Means | What the app tells the user |
| --- | --- | --- |
| `TO AGREE` | Not your locker | `TO AGREE` |
| `TO AGREE` | Wrong mode for this locker | `TO AGREE` |
| `TO AGREE` | Locker faulty or offline | `TO AGREE` |
| `TO AGREE` | Token expired | (send to login, no message needed) |
| `TO AGREE` | Card code not known | `TO AGREE` |
| `TO AGREE` | Parcel code already used or expired | `TO AGREE` |

## Open questions for the Server team

1. Does endpoint 4 include each point's location, so the app can find the nearest? (blocks P3-04)
2. What exactly does endpoint 8 receive — the raw barcode digits, or something derived? (blocks P5-05)
3. Is the mode per locker point, or one mode for the whole school? (blocks P6-01)
4. How long does a token last, and is there a refresh call? (blocks P2-06)
5. What is the exact wording of the password email, so the app's first screen matches it? (blocks P0-07)
6. Does the server ever push to the app, or does the app always ask? (affects P3-07)
