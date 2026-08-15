# API contract — front-ends ↔ server

**Status: OUR PROPOSAL. Written, not yet sent.** Sending it is task **P0-04**.

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
| 19 | **Sign in with Google, or link Google to this account** | `POST /auth/google` | Google ID token, and a receiver token when linking | token, expiry, or a refusal code |

**Endpoint 19 never makes an account.** A Google account has no phone number, and the shipper finds the receiver by phone number at the cabinet — so an account made from Google alone could never be sent a parcel. Google is a faster way back into an account that a one-time code already proved.

One path, told apart by whether a receiver token is sent:

| Sent | Means | Answer |
| --- | --- | --- |
| Google ID token only | "Let me in as whoever this Google account belongs to" | The token, when that Google account has been linked. `PHONE_REQUIRED` when it has not — the app then falls back to endpoints 1 and 2 |
| Google ID token **and** a receiver token | "I am signed in as this phone. Remember this Google account for it" | The token. Both proofs are on the wire at once, so nothing has to be remembered between two calls |

| 20 | Set or change the password | `POST /auth/set-password` | token, new password | ok, or a refusal code |
| 21 | Sign in with a password | `POST /auth/password-login` | phone number, password | token, expiry, or a refusal code |

**Endpoints 20 and 21 carry no email address, and there is no register-with-a-password route.** A password is set on an account a one-time code already proved, and you sign in with the phone number. Same reason as endpoint 19: an account identified by an email could never be sent a parcel, because the shipper types a phone number. Decided in [ADR 0012](../adr/0012-passwords-on-a-phone-account.md). Task **P2-09**.

**There is no password reset endpoint and no reset email.** Forgetting a password is endpoints 1 and 2, then endpoint 20 — calls that already exist. Setting a password clears any lockout, because the person just proved themselves another way.

**Hashing is Argon2id** at OWASP's minimum, with the parameters stored inside each hash so they can be raised later without stranding what came before. **Five wrong tries lock the account for fifteen minutes, and that counter is on disk** — a counter in memory resets when the process dies, so anyone who can crash the server gets their guesses back.

**The server checks the ID token itself, offline** — signature against Google's published keys, `iss`, `aud` equal to our own client id, and `exp`. It never asks Google's tokeninfo endpoint, which would put the network and a rate limit on the login path. The account is keyed on Google's `sub`, never on the email address, because a person can change their Gmail address. Task **P2-08**.

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

## Endpoints — when the network is gone

Decided by [ADR 0004](../adr/0004-offline-pickup.md), permitted by [ADR 0010](../adr/0010-c4-offline-exception.md). Task **P0-12**.

The cabinet is on Wi-Fi, and campus Wi-Fi drops. These two calls are what makes a pickup still work, and **neither of them happens during the outage** — that is the point. They set it up beforehand and settle it afterwards.

| # | What the caller wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 16 | Give this phone its offline secret | `POST /auth/offline-secret` | token, cabinet ref | the secret, and which cabinet it is for |
| 17 | Here is what happened while I was offline | `POST /cabinet/reconcile` | cabinet key, a list of events | which were accepted |

**Endpoint 16 is called once, at registration, per cabinet.** The server works out `secret = HMAC(cabinet key, phone number)` and the app stores it in the phone's secure store. The person never sees it. If a second cabinet is added, the app holds a second secret — assumption **A-12**.

**Endpoint 17 is not optional.** An outage must never lose the record of who opened what. Everything the cabinet did alone is sent up the moment the network returns, and the server is the truth again from that instant.

### The exchange itself — no server, no network

Nothing here is an API call. Both sides already hold what they need.

```
   ①  she types her phone number on the cabinet
   ②  cabinet picks a random challenge, shows it as a QR, remembers it
   ③  her phone reads it → HMAC(her secret, challenge + box number) → 6 digits
   ④  she types the 6 digits
   ⑤  cabinet works out the same value and compares → the door opens
   ⑥  that challenge is thrown away. It never works twice
```

| Rule | Value |
| --- | --- |
| Algorithm | **HMAC-SHA256**, truncated to the last 6 digits. *Fixed — published on purpose* |
| What is signed | `challenge ‖ box number`. The box number is in it, so an answer for one box is not an answer for another |
| Secret | `HMAC(cabinet key, phone number)`. One key **per cabinet**, never one shared master. *Fixed* |
| Challenge | Fresh random per attempt, ≥ 128 bits, discarded after one use. *Fixed* |
| Clocks | **Not used.** The cabinet supplies the randomness, so no clock has to agree with any other |
| Wrong tries | The same `wrong_tries_before_lock` and `box_lock_minutes` as the typed code. **The count must survive a power cut** |

**No secret algorithm.** It ships inside an app on students' phones and will be taken apart. Kerckhoffs's principle: the key is the secret, never the method. See ADR 0004, which records this as one of three things the first sketch got wrong.

**A wrong answer says the same thing as a wrong-and-expired one** — the `CODE_REJECTED` rule below applies here unchanged.

**Offline, a drop skips the name check.** The cabinet cannot look anything up, so it takes the number, opens a box, and endpoint 17 settles it later. The screen says so plainly, so the driver knows to read the number twice.

## Endpoints — both callers

| # | What the caller wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 15 | Get the current settings | `GET /settings` | token, or cabinet key | `settings_version`, and the numbers below it |
| 18 | **Which boxes at a cabinet are free** | `GET /cabinet/free` | token, cabinet ref | how many boxes exist, and **which numbers are free** |

**Endpoint 18 is new, and the app cannot draw its cabinet screen without it.** The receiver's Cabinet tab shows the real cabinet with the free doors marked, so a student can tell a shipper *"use the back gate one, it has room"* before the shipper walks over. Endpoint 5 lists only that receiver's own parcels, which cannot answer it. Task **P4-06**.

Two rules on what it may return, both from [architecture.md](architecture.md) rule 6:

| Rule | Why |
| --- | --- |
| **A free box number, or nothing.** Never who is in a taken box, never a name, never a phone number | A door that is taken is drawn as taken and nothing more. The list of free numbers is the whole answer |
| **A count is enough when the cabinet is full** | Returning nineteen taken numbers tells an attacker the cabinet's occupancy pattern over time. `free: []` says all that is needed |

Every number in this contract is a guess we expect to correct. Endpoint 15 is how a correction reaches a phone that is already installed. Without it, every number is frozen at whatever shipped. Task **P1-08**.

| Rule | Value |
| --- | --- |
| What it returns | The same keys as `config/settings.json`, minus the ones marked fixed below |
| Which copy wins | The one with the higher `settings_version` |
| If the call fails | Keep the last good copy. Never fall back to nothing |
| First run, no network | The copy that shipped with the app |
| `server_base_url` | **Never returned.** *Fixed — not a setting the server may set* |

**`server_base_url` is the one key the server may not send.** The app has to know the address before it can ask, so a server-set address is circular. Worse, it hands anybody who answers that call the power to point the app somewhere else. It stays in the shipped file and changes only with a release.

**This endpoint carries numbers, never rules.** `qr_session_seconds` may change from 60 to 120. "The typed code works once" may not — that is a shape, and a shape change is a contract change needing both teams. The keys marked *Fixed* in this file are the list.

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

The scanned path needs none of *these* rules, because its QR is not a key. It needs its own, and they are below.

## The scanned code — what protects it

Path D1. The QR is not a secret and is not meant to be: photograph the cabinet screen and you have *which cabinet, at what moment*, which opens nothing. The token proves who is asking. That much was always the design — [ADR 0003](../adr/0003-parcel-locker-product.md).

What was missing from this file is what the **server** must check, and it matters more than it sounds. The app checks the code's age and its cabinet before it ever calls endpoint 6, and **none of those checks protect anything**: they run on the caller's phone, and an attacker does not run our app. They call `POST /parcels/collect` directly. Every rule below therefore has to be enforced on the server, or it is not enforced.

| Rule | Why |
| --- | --- |
| The session code must be one **this server issued** at endpoint 9, and still inside its window, **timed by the server's clock** | Otherwise the code can be invented. The server has to remember what it handed out; the code cannot be self-describing |
| The parcel must be **at the cabinet the code names** | Without this, a code from any cabinet opens any box of yours, and being at the locker stops being necessary at all |
| The parcel must belong to **the token in the same request** | This is the one that stops somebody opening another person's box. It is also the only rule the QR itself can never carry |
| One open per session code, per parcel | A captured request is otherwise replayable for as long as the code lives |
| A refusal never says **why** beyond the reasons below | "No parcel here" and "not yours" must read the same to somebody probing |

**What this means if the server skips the first two.** A valid token plus any string would collect a parcel — from a sofa, at any hour, with no cabinet involved. The scan would be decoration. Nothing in the app can prevent that, which is why it is written down here rather than left to be obvious.

## Refusal reasons

Every failure the server can send, with the code we propose and the exact words the screen shows. The words are the only part of this file a user ever reads, so they are ours to get right.

**Shortened on 2026-08-07, before this contract was sent.** The first draft explained each failure in a sentence and a half. Nobody reads a sentence and a half on a phone they are holding while walking, and the cabinet screen is read at arm's length by somebody with a parcel under one arm. Every line below now says what happened and what to do, in that order, and stops. What did not change is what each one is *allowed* to say — see the two rules underneath.

| Code | Means | Who sees it | What the screen says |
| --- | --- | --- | --- |
| `PHONE_NOT_REGISTERED` | Phone number is not registered | Cabinet | "Not signed up yet. Ask them to install the app." |
| `NO_FREE_BOX` | No free box of that size | Cabinet | "No box this size. Try a smaller one." |
| `BOX_FAULTY` | Box faulty or offline | Both | "That box is out of order. Staff know." |
| `BOX_ALREADY_FULL` | A box thought to be free is occupied | Cabinet | "That box is not empty. Tell staff." |
| `SESSION_EXPIRED` | QR session code expired | App | "Code expired. Scan again." |
| `NO_PARCEL_HERE` | No parcel for you at this cabinet | App | "Nothing waiting here." |
| `TOKEN_EXPIRED` | Token expired | App | *(send to register, no message)* |
| `WRONG_CODE` | Wrong one-time code, at register | App | "Wrong code. Try again." |
| `CODE_REJECTED` | Wrong, used, or expired pickup code | Cabinet | "That code did not work. Scan with the app instead." |
| `BOX_LOCKED_OUT` | Too many wrong tries | Cabinet | "Too many tries. Unlocks at 14:35." |

**The reference server (docs/superpowers/specs/2026-08-09-otp-sender-design.md) adds three codes for auth: `PHONE_INVALID` (the number is not a Vietnamese mobile), `RATE_LIMITED` (a code was requested within the last 60 seconds) and `SEND_FAILED` (the SMS provider did not confirm delivery). The app's Refusal enum does not name them; they fall to `UNKNOWN`, which shows the generic sentence. `WRONG_CODE` and `TOKEN_EXPIRED` are already in the table.**

**Endpoints 20 and 21 add three more.** `PASSWORD_TOO_SHORT` and `PASSWORD_TOO_LONG` are the only rules on a password, and they come back from endpoint 20 only. `WRONG_PASSWORD` is the single answer endpoint 21 ever gives: it covers a wrong password, a locked-out account, and a phone number nobody has registered, on purpose. Telling them apart would answer two questions a sign-in screen must not answer — whether that number is a user here, and whether the lockout has started. It is the same rule `WRONG_CODE` already follows.

**Endpoint 19 adds three more.** `PHONE_REQUIRED` is not a failure — it is the app's cue to ask for a phone number and a one-time code, then call endpoint 19 again with the receiver token to link. `GOOGLE_INVALID` covers every way an ID token can be wrong, in one code and on purpose: telling a caller *which* check failed helps only somebody probing it. `GOOGLE_OFF` means no client id is configured on that server, so the app must hide the Google button rather than show one that cannot work.

**`CODE_REJECTED` covers three different failures on purpose.** Wrong, already used, and expired all return the same code and the same words. Splitting them would let somebody at the keypad work out which codes are real.

**A wrong typed code must not say whether that code exists.** "Not right" and "not right yet" are the same message. Anything else lets somebody at the keypad work out which codes are real.

## What we still need from the Server team

Not blanks. Three things only they can answer, and one thing we need a yes to.

1. **Can you build this?** If any endpoint above is awkward on your side, say which and why. We would far rather change it now than after the screens exist.
2. **How does the cabinet get its key, and how is it replaced if a cabinet is stolen?** The one design question we cannot answer for you — it depends on how you issue credentials. (blocks P0-05)
3. **What does endpoint 10 return when two people share one phone number?** Rare, but it decides whether the shipper sees a list or an error. (blocks P3-03)
4. **Confirm the masked-name format.** We propose `Nguyễn V. A***`. If your data cannot produce that shape, tell us what it can. (blocks P0-08)

Everything else in this file is a number, and every number lives in the settings file. Want a different value? Change the setting. No meeting needed — once endpoint 15 exists and task **P1-08** is ticked. Until then a changed number needs a new release, so endpoint 15 is worth building early.
