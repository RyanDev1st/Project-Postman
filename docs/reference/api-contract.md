# API contract — front-ends ↔ server

**Status: OUR PROPOSAL. Sent, not yet answered.**

The Server team does not have an API design yet. So this is not a form with blanks for them to fill — it is **our complete draft**, with every value chosen. Their job is to read it and say what is wrong. See [ADR 0005](../adr/0005-we-propose-they-object.md).

Every number in here is a **guess with a default**, and every one of them lives in the settings file (task **P0-15**) so correcting it costs five minutes, not a release. See the settings table in [architecture.md](architecture.md).

Still the most expensive document in the project. If the **shape** changes after the screens are built, the screens are rebuilt. A number changing costs nothing. Task **P0-04**.

Product: a parcel drop-off locker. See [ADR 0003](../adr/0003-parcel-locker-product.md) and [architecture.md](architecture.md).

## How to use this file

- **We wrote all of it.** Paths, fields, error codes, numbers. Nothing is left blank.
- **The Server team reads it and objects.** Anything they cannot build, or would build differently, they say so and we change it.
- **Silence means agreement.** We build against this until told otherwise.
- **Once they have agreed a value, it stops being ours to change alone.** A change after that needs both teams — rule C2.

Paths are a suggestion and cost almost nothing to change: every call in the app goes through one file (task P1-04), so renaming them all is a ten-minute job.

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
| Transport | HTTPS only. No plain HTTP, not even in test. **Fixed, not a setting** |
| Format | JSON |
| Auth — app | Receiver token in the request header, after the one-time code |
| Auth — cabinet | Cabinet key in the request header, on every call |
| Time | UTC, ISO 8601, everywhere. **Our proposal** |
| Errors | Every failure has a stable code the front-end can switch on, plus a message the server does **not** expect it to show raw |

## Endpoints — the phone app

### Register and identity

| # | What the app wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 1 | Ask for a one-time code | `POST /auth/request-code` | phone number | sent, or a refusal code |
| 2 | Send the one-time code back | `POST /auth/verify-code` | phone number, code | token, expiry |
| 3 | Refresh or check the token | `POST /auth/refresh` | token | new token, or not valid |
| 4 | Log out | `POST /auth/logout` | token | ok |

### Parcels and pickup

| # | What the app wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 5 | List my waiting parcels | `GET /parcels` | token | cabinet name, box number, time it arrived |
| 6 | **Pick up by scanning — the main path** | `POST /parcels/collect` | token, QR session code | which box opened, or a refusal code |
| 7 | My history | `GET /parcels/history` | token | list of time, box, action |
| 8 | Register this phone for notifications | `POST /devices` | token, device id | ok |

## Endpoints — the cabinet screen

| # | What the cabinet wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 9 | Get the QR session code to display | `GET /cabinet/session` | cabinet key | session code, how long it lives |
| 10 | Look up a receiver by phone number | `GET /cabinet/receiver` | cabinet key, phone number | **masked** name, or a refusal code |
| 11 | Start a drop | `POST /cabinet/drop` | cabinet key, receiver ref, parcel size | which box opened, or a refusal code |
| 12 | **A door closed** | `POST /cabinet/door-closed` | cabinet key, box number, drop or collect | recorded, and what the server did next |
| 13 | **Pick up by typed code — the backup path** | `POST /cabinet/collect-by-code` | cabinet key, the typed code | which box opened, or a refusal code |
| 14 | Report a faulty box | `POST /cabinet/fault` | cabinet key, box number, what happened | ok |

**Endpoint 10 returns a masked name.** `Nguyễn V. A***`, never the full name. The shipper already knows who he is delivering to — he only needs to confirm he has the right person. Without masking, anyone can stand at the cabinet, type phone numbers, and collect names. See task P0-08.

**Endpoint 12 is named for the event, not for what causes it.** There is no sensor — see [ADR 0006](../adr/0006-no-sensor.md) — so today a closing door fires it. If a sensor is fitted later, the sensor fires the same call and **nothing else in this contract changes**. That is why it is called *a door closed* and not *a sensor reading*.

The cost of having no sensor is written down in ADR 0006: a driver can record a delivery without leaving the parcel. It is traceable afterwards, not preventable beforehand, and we accept that.

**Endpoint 13 is the one call with no token behind it.** Everything else proves who the user is from a login. This one proves it from the code alone — so the code carries the whole weight, and the rules below are not optional.

## The typed code — what protects it

Path D2 in [architecture.md](architecture.md). It exists so a flat battery does not trap a parcel. It is the back door, and it is defended like one.

| Rule | Value |
| --- | --- |
| Works how many times | **Once.** After a box opens, the code is dead. *Fixed — not a setting* |
| Lifetime | **48 hours.** Adjustable |
| Wrong tries before lockout | **5**, then that box refuses codes for **15 minutes**. Adjustable |
| Length | **6 digits.** Adjustable, but read [ADR 0004](../adr/0004-offline-pickup.md) before shortening it |
| Travels how | With the notice, by push. Adjustable |
| Reused across parcels | **Never.** A new parcel gets a new code. *Fixed — not a setting* |

The scanned path (endpoint 6) needs none of this, because its QR is not a key — it only says *which cabinet, right now*, and the token proves who is standing there.

## Refusal reasons

Every failure the server can send, with the code we propose and the exact words the screen shows. The words are the only part of this file a user ever reads, so they are ours to get right.

| Code | Means | Who sees it | What the screen says |
| --- | --- | --- | --- |
| `PHONE_NOT_REGISTERED` | Phone number is not registered | Cabinet | "This number has not signed up yet. Ask them to install the app first." |
| `NO_FREE_BOX` | No free box of that size | Cabinet | "No free box this size right now. Try a smaller one, or come back later." |
| `BOX_FAULTY` | Box faulty or offline | Both | "That box is out of order. Staff have been told." |
| `BOX_ALREADY_FULL` | A box thought to be free is occupied | Cabinet | "Something is already in that box. Please tell staff." |
| `SESSION_EXPIRED` | QR session code expired | App | "That code has expired. Scan the screen again." |
| `NO_PARCEL_HERE` | No parcel for you at this cabinet | App | "You have nothing waiting at this cabinet." |
| `TOKEN_EXPIRED` | Token expired | App | *(send to register, no message)* |
| `WRONG_CODE` | Wrong one-time code, at register | App | "That code is not right. Check the message and try again." |
| `CODE_REJECTED` | Wrong, used, or expired pickup code | Cabinet | "That code did not work. Open the app and scan the screen instead." |
| `BOX_LOCKED_OUT` | Too many wrong tries | Cabinet | "Too many wrong codes. This box unlocks again at 14:35." |

**`CODE_REJECTED` covers three different failures on purpose.** Wrong, already used, and expired all return the same code and the same words. Splitting them would let somebody at the keypad work out which codes are real.

**A wrong typed code must not say whether that code exists.** "Not right" and "not right yet" are the same message. Anything else lets somebody at the keypad work out which codes are real.

## What we still need from the Server team

Not blanks. Three things only they can answer, and one thing we need a yes to.

1. **Can you build this?** If any endpoint above is awkward on your side, say which and why. We would far rather change it now than after the screens exist.
2. **How does the cabinet get its key, and how is it replaced if a cabinet is stolen?** The one design question we cannot answer for you — it depends on how you issue credentials. (blocks P0-05)
3. **What does endpoint 10 return when two people share one phone number?** Rare, but it decides whether the shipper sees a list or an error. (blocks P3-03)
4. **Confirm the masked-name format.** We propose `Nguyễn V. A***`. If your data cannot produce that shape, tell us what it can. (blocks P0-08)

Everything else in this file is a number, and every number lives in the settings file. Want a different value? Change the setting. No meeting needed.
