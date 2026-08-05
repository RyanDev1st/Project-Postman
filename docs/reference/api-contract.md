# API contract — front-ends ↔ server

**Status: DRAFT. Not agreed.** Every `TO AGREE` marker is an open question for the Server team.

This is the most expensive document in the project. If it changes after the screens are built, the screens are rebuilt. Settle it in Phase 0. See task **P0-04**.

Product: a parcel drop-off locker. See [ADR 0003](../adr/0003-parcel-locker-product.md) and [architecture.md](architecture.md).

## How to use this file

- The **app team** writes what the front-ends need.
- The **Server team** writes what the server can give.
- Both teams read it together, remove every `TO AGREE`, and agree it.
- After that, a change to this file is a change both teams agree to first.

## Two callers, not one

The server answers two different callers. They are **not** allowed the same calls.

| Caller | Who holds it | Signs in as | May do |
| --- | --- | --- | --- |
| **Phone app** | The receiver | A person, with a token | Register, list own parcels, pick up |
| **Cabinet screen** | Nobody. It is a public terminal | A device, with a cabinet key | Look up masked name, start a drop, confirm a door |

**The cabinet key never sits in this repo.** It is placed on the cabinet during setup. See task P0-05.

A call from the cabinet may never return a full name, a phone number, or a list of parcels. See rule 6 in `architecture.md`.

## Ground rules

| Rule | Value |
| --- | --- |
| Transport | HTTPS only. No plain HTTP, not even in test |
| Format | JSON |
| Auth — app | Receiver token in the request header, after the one-time code |
| Auth — cabinet | Cabinet key in the request header, on every call |
| Time | One time format everywhere — `TO AGREE` (suggest UTC, ISO 8601) |
| Errors | Every failure has a stable code the front-end can switch on, plus a message the server does **not** expect it to show raw |

## Endpoints — the phone app

### Register and identity

| # | What the app wants | Path | Sends | Gets back | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | Ask for a one-time code | `TO AGREE` | phone number | sent, or a refusal code | `TO AGREE` |
| 2 | Send the one-time code back | `TO AGREE` | phone number, code | token, expiry | `TO AGREE` |
| 3 | Refresh or check the token | `TO AGREE` | token | valid or not | `TO AGREE` |
| 4 | Log out | `TO AGREE` | token | ok | `TO AGREE` |

### Parcels and pickup

| # | What the app wants | Path | Sends | Gets back | Status |
| --- | --- | --- | --- | --- | --- |
| 5 | List my waiting parcels | `TO AGREE` | token | cabinet name, box number, time it arrived | `TO AGREE` |
| 6 | **Pick up — the core call** | `TO AGREE` | token, QR session code | which box opened, or a refusal code | `TO AGREE` |
| 7 | My history | `TO AGREE` | token | list of time, box, action | `TO AGREE` |
| 8 | Register this phone for notifications | `TO AGREE` | token, device id | ok | `TO AGREE` |

## Endpoints — the cabinet screen

| # | What the cabinet wants | Path | Sends | Gets back | Status |
| --- | --- | --- | --- | --- | --- |
| 9 | Get the QR session code to display | `TO AGREE` | cabinet key | session code, how long it lives | `TO AGREE` |
| 10 | Look up a receiver by phone number | `TO AGREE` | cabinet key, phone number | **masked** name, or a refusal code | `TO AGREE` |
| 11 | Start a drop | `TO AGREE` | cabinet key, receiver ref, parcel size | which box opened, or a refusal code | `TO AGREE` |
| 12 | A door closed | `TO AGREE` | cabinet key, box number | stored, and the notice was sent | `TO AGREE` |
| 13 | Report a faulty box | `TO AGREE` | cabinet key, box number, what happened | ok | `TO AGREE` |

**Endpoint 10 returns a masked name.** `Nguyễn V. A***`, never the full name. The shipper already knows who he is delivering to — he only needs to confirm he has the right person. Without masking, anyone can stand at the cabinet, type phone numbers, and collect names. See task P0-09.

## Refusal reasons

Each front-end must show a different plain message for each one. List every reason the server can send. `TO AGREE`.

| Code | Means | Who sees it | What the screen says |
| --- | --- | --- | --- |
| `TO AGREE` | Phone number is not registered | Cabinet | `TO AGREE` |
| `TO AGREE` | No free box of that size | Cabinet | `TO AGREE` |
| `TO AGREE` | Box faulty or offline | Both | `TO AGREE` |
| `TO AGREE` | QR session code expired | App | `TO AGREE` (rescan, no drama) |
| `TO AGREE` | No parcel for you at this cabinet | App | `TO AGREE` |
| `TO AGREE` | Token expired | App | (send to register, no message needed) |
| `TO AGREE` | Wrong one-time code | App | `TO AGREE` |
| `TO AGREE` | Too many code attempts | App | `TO AGREE` |

## Open questions for the Server team

1. **Does the cabinet have its own network connection?** Every call above assumes it does. (blocks P0-03 — ask this first)
2. How does the receiver get the notification — push through the app, Zalo, or SMS? (blocks P0-06)
3. How long does the QR session code live, and how often does the screen refresh it? (blocks P0-07)
4. How does the cabinet get its key, and how is that key replaced if a cabinet is stolen? (blocks P0-05)
5. What exactly does endpoint 10 return when two people share one phone number? (blocks P3-03)
6. How long does a receiver token last, and is there a refresh call? (blocks P2-04)
7. What happens to a parcel nobody collects? Who is told, and when? (blocks P6-03)
8. Can one receiver have two parcels at one cabinet at the same time? (blocks P6-02)
