# API contract — front-ends ↔ server

**Status: BUILT.** As of 2026-08-15 this is not a proposal to anybody — it is what `src/server/` serves. [ADR 0019](../adr/0019-we-own-the-server.md): we own the server, because the team that was going to build it builds hardware.

That changes what this file is for. It was a draft to be argued with; it is now the description of a running thing, and **the code and this page must agree**. If they ever disagree, the code is what students meet, so fix whichever is wrong in the same change — never one alone.

What has *not* changed is that every number here is a guess with a default, living in `config/settings.json`. See the settings table in [architecture.md](architecture.md).

Endpoints 3 to 15 and 18 are built and checked by `python scripts/checkserver.py`. Endpoints 16, 17, 20 and 21 are written down here and **not built**. Endpoint 19 is built but does not yet make an account or check `hd`.

**Changed 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md), and not yet built.** Endpoints 1 and 2 are retired, 19 becomes the way in, and 25 to 29 are new. `checkserver.py` still walks the old shape and will fail against the new one — that is expected until **P2-14**, and the rewrite of that script goes with it.

Every number in here is a **guess with a default**, and every one of them lives in the settings file (task **P0-15**) so correcting it costs five minutes, not a release. See the settings table in [architecture.md](architecture.md).

Still the most expensive document in the project. If the **shape** changes after the screens are built, the screens are rebuilt. A number changing costs nothing. Task **P0-04**.

Product: a parcel drop-off locker. See [ADR 0003](../adr/0003-parcel-locker-product.md) and [architecture.md](architecture.md).

## How to use this file

- **We wrote all of it, and we serve all of it.** Paths, fields, error codes, numbers.
- **A shape change is a three-sided change.** The server writes it, the app reads it, and the cabinet screen carries it. Changing one and not the others is how a scan starts failing with nothing in either build saying why.
- **The hardware team gets the part that touches them**, and only that: [cabinet-firmware.md](cabinet-firmware.md). Two calls, one key, one rule.

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
| Auth — app | Receiver token in the request header, issued by Google sign-in at endpoint 19 |
| Auth — cabinet | Cabinet key in the request header, on every call |
| Time | UTC, ISO 8601, everywhere. **Our proposal** |
| Errors | Every failure has a stable code the front-end can switch on, plus a message the server does **not** expect it to show raw |

## Endpoints — the phone app

### Register and identity

| # | What the app wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| ~~1~~ | ~~Ask for a one-time code~~ | ~~`POST /auth/request-code`~~ | — | **Removed 2026-09-02.** [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md) |
| ~~2~~ | ~~Send the one-time code back~~ | ~~`POST /auth/verify-code`~~ | — | **Removed 2026-09-02.** [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md) |
| 3 | Refresh or check the token | `POST /auth/refresh` | token | new token, or not valid |
| 4 | Log out | `POST /auth/logout` | token | ok |
| 19 | **Sign in with Google. This is the way in** | `POST /auth/google` | Google ID token | token, expiry, or a refusal code |

**Numbers 1 and 2 are retired, not reused.** They are struck through above rather than deleted so that a build reading an older copy of this file, or a log line naming an endpoint, still resolves to something. Nothing new ever takes those numbers.

**Endpoint 19 makes the account.** This reverses [ADR 0011](../adr/0011-google-sign-in-no-passwords.md), which forbade it, and the reason 0011 gave — a Google account has no phone number, so nobody could ever send it a parcel — is answered by the booking at endpoint 25. See [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md).

The account is keyed on Google's `sub`, never on the email address, because a person can change their address and `sub` outlives it.

**The domain is the gate.** The ID token must carry an `hd` claim on a VGU domain:

| `hd` | Answer |
| --- | --- |
| `vgu.edu.vn` | Staff and teachers. Allowed |
| `student.vgu.edu.vn`, or anything else under `.vgu.edu.vn` | Students. Allowed |
| Absent | Refused with `GOOGLE_DOMAIN`. A personal Gmail carries no `hd`, so requiring it is what keeps personal accounts out |
| Anything else | Refused with `GOOGLE_DOMAIN` |

The test is `hd == "vgu.edu.vn" || hd.endsWith(".vgu.edu.vn")`. **The leading dot is the check**: without it, `endsWith("vgu.edu.vn")` also accepts `notvgu.edu.vn`, which anybody can register for a few dollars. The allowed domains live in `google_allowed_domains` in `config/settings.json`, so a new subdomain is a text edit rather than a release. The claim read is `hd` and never the email address, which can be an alias.

A signed-in account with no phone number yet is a normal state, not an error. It can read its own parcels and its own settings; it cannot be sent a parcel until it books at endpoint 25.

| # | What the app wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 20 | Set or change the password | `POST /auth/set-password` | token, new password | ok, or a refusal code |
| 21 | Sign in with a password | `POST /auth/password-login` | phone number, password | token, expiry, or a refusal code |

**Endpoints 20 and 21 carry no email address, and there is no register-with-a-password route.** A password is set on an account that already exists, and you sign in with the phone number that account booked with. Decided in [ADR 0012](../adr/0012-passwords-on-a-phone-account.md). Task **P2-09**.

**There is no password reset endpoint and no reset email.** Forgetting a password is endpoint 19, then endpoint 20 — sign in with Google, set a new one. Until 2026-09-02 that path was endpoints 1 and 2; the shape is unchanged, only the door. Setting a password clears any lockout, because the person just proved themselves another way.

**Endpoint 21 needs an account that has booked at least once.** Signing in by password takes a phone number, and an account that has never booked has none. Such an account signs in with Google, which it must have to exist at all.

**Hashing is Argon2id** at OWASP's minimum, with the parameters stored inside each hash so they can be raised later without stranding what came before. **Five wrong tries lock the account for fifteen minutes, and that counter is on disk** — a counter in memory resets when the process dies, so anyone who can crash the server gets their guesses back.

**The server checks the ID token itself, offline** — signature against Google's published keys, `iss`, `aud` equal to our own client id, `exp`, and now `hd`. It never asks Google's tokeninfo endpoint, which would put the network and a rate limit on the login path. Task **P2-08**; the domain check is **P2-11**.

### Booking a box

The receiver books before the parcel arrives. This is what makes the phone number trustworthy without an SMS ever being sent: the number can only be typed by somebody signed in on a VGU domain, against a delivery they are expecting themselves, so a wrong digit breaks their own delivery and nobody else's. [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md).

| # | What the app wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 25 | **Book a box** | `POST /bookings` | token, **cabinet ref**, parcel size, and a phone number **only the first time** | cabinet ref, box number, the number as stored, expiry, or a refusal code |
| 26 | My booking | `GET /bookings` | token | the live booking — cabinet ref, box number, the number as stored, expiry — or nothing |
| 27 | Cancel my booking | `DELETE /bookings` | token | ok |
| 29 | **Change my number** | `PUT /me/phone` | token, phone number | the number as stored, or a refusal code |

**Endpoint 25 takes a cabinet ref, because there is more than one cabinet.** A booking holds a door at a named cabinet, and a parcel dropped at a different one has no booking to match — it falls to rung B of the ladder below and takes any free box there. The app reads the free doors at each cabinet from endpoint 18 before booking, which is what that endpoint was built for.

**`NO_FREE_BOX` is returned here too**, and it means at booking time rather than at drop time. Asked for `large` when none is free, the answer is the refusal and not a quiet `small` — same rule endpoint 11 already follows, for the same reason: only the person holding the parcel knows whether a smaller door will do.

| Rule | Value |
| --- | --- |
| What a booking holds | **One specific box.** Reserved, and no other drop may take it |
| How long | **24 hours**, then it expires and the box is free. Adjustable |
| How many at once | **One per account.** A second call while one is live is refused with `BOOKING_EXISTS` |
| The number | Typed **once**, then kept on the account. Stored in one form by `Phone.normalise` — `+84` and nine digits |
| Later bookings | Send no number. The account already has one, and re-typing it every time is a fresh chance to mistype it |
| A number already claimed | Refused with `PHONE_IN_USE`, naming nobody. First claim wins |

**The number is asked for once and belongs to the account after that.** A student books their second parcel by choosing a cabinet, a size and nothing else. This is not only convenience: every re-typing is another chance to introduce the typo this whole ladder exists to survive, and a number that is entered once is a number that can be checked once and then trusted.

**Endpoint 25 returns the number it stored, not the number it was sent.** `0908619328`, `908619328` and `+84908619328` all store as `+84908619328`, and the app shows that back on a confirm panel before the first booking is accepted. A panel that repeats the same shape somebody just typed is one the eye slides over, and a typo here is silent — there is no code arriving to contradict it. Task **P2-13**.

**Endpoint 29 is how a wrong number gets fixed**, and it is the other half of the notice at the end of this section. Somebody told *"the courier's label said `…382`, you booked `…328`"* needs one tap that corrects it. It refuses with `PHONE_IN_USE` on the same rule as the first claim, and it does **not** move a live booking to the new number — the parcel already on its way was addressed to the old one.

### Parcels and pickup

| # | What the app wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 5 | List my waiting parcels | `GET /parcels` | token | cabinet name, box number, time it arrived |
| 6 | **Pick up by scanning — the main path** | `POST /parcels/collect` | token, QR session code | which box opened, or a refusal code |
| 7 | My history | `GET /parcels/history` | token | list of time, box, action, cabinet name |
| 8 | Register this phone for notifications | `POST /devices` | token, device id | ok |
| 24 | **Who is signed in** | `GET /me` | token | the account's own full name and phone number |

## Endpoints — the cabinet screen

| # | What the cabinet wants | Path we propose | Sends | Gets back |
| --- | --- | --- | --- | --- |
| 9 | Get the QR session code to display | `GET /cabinet/session` | cabinet key | session code, how long it lives |
| 10 | Look up a receiver by phone number | `GET /cabinet/receiver` | cabinet key, phone number | **masked** name and any booking, a **near miss**, or a refusal code |
| 11 | Start a drop | `POST /cabinet/drop` | cabinet key, receiver ref, parcel size | which box opened, or a refusal code |
| 28 | **Is this the name on the parcel?** | `POST /cabinet/confirm-name` | cabinet key, the number already typed, the name off the label | one **masked** name and its box, or nothing. **Never a list, never a number** |
| 12 | **A door closed** | `POST /cabinet/door-closed` | cabinet key, box number, drop or collect | recorded, and what the server did next. **The server works the purpose out itself** and logs a disagreement — see below |
| 13 | **Pick up by typed code — the backup path** | `POST /cabinet/collect-by-code` | cabinet key, the typed code | which box opened, or a refusal code |
| 14 | Report a faulty box | `POST /cabinet/fault` | cabinet key, box number, what happened | ok |
| 22 | **Anything for me to do?** | `GET /cabinet/commands` | cabinet key | a list of doors to open, each with an id and **why it is opening** — `drop` or `collect` |
| 23 | That is done | `POST /cabinet/command-done` | cabinet key, command id, result | ok |

**Endpoints 22 and 23 are the ESP32's, and were added with [ADR 0020](../adr/0020-the-server-is-kotlin.md).** The board cannot be dialled — it takes a DHCP address on campus Wi-Fi and has no name — so it asks once a second rather than being told. The cost is up to a second before a door opens, which a person standing at a locker will accept.

**A command is handed over exactly once.** The row is stamped as taken inside the same transaction that reads it, so a board that loses the reply and asks again is handed nothing. That is where "an open never comes from a retry" stops being a wish. Endpoint 23 is for the record only and nothing waits on it: a cabinet that loses power between opening a door and saying so must not leave somebody standing at an open box being told it failed.

The full protocol, and a reference sketch, are in [cabinet-firmware.md](cabinet-firmware.md).

**Endpoint 10 returns a masked name.** `Nguyễn V. A***`, never the full name. The shipper already knows who he is delivering to — he only needs to confirm he has the right person. Without masking, anyone can stand at the cabinet, type phone numbers, and collect names. See task P0-08.

**Endpoints 10, 28 and 11 are a ladder, and the shipper walks down it.** Added 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md). The number the shipper types came off a parcel label, which came from whatever the receiver typed into a shop — so a miss is far more often a typo than a stranger.

| Rung | The number | What the screen does | Task |
| --- | --- | --- | --- |
| A | Matches a **live booking** exactly | Shows the masked name. Endpoint 11 opens **that booking's box and no other** | P3-08 |
| B | Matches a **registered account with no booking** exactly | Shows the masked name. Endpoint 11 takes any free box, as it does today | P3-08 |
| C | Is **within two digits** of exactly one live booking | Offers that one booking: *"Did you mean Nguyễn V. A***?"* | P3-09 |
| D | Is within two digits of **more than one**, or of none | Asks the shipper to **type the name on the parcel**, endpoint 28 | P3-10 |
| E | The name matches none, or more than one | `PHONE_NOT_REGISTERED`. Contact the recipient, and leave the parcel at **ABO**, the grocery store facing the campus back gate | P3-11 |

**Two digits, measured rather than guessed.** The distance is Damerau-Levenshtein over the nine national digits, so a substitution, an insertion, a deletion and a transposition each cost one. Simulated against twenty live bookings with real Vietnamese operator prefixes: at a tolerance of two, a receiver who mistyped one digit is found **100%** of the time, one who mistyped two is found **100%** of the time, and a parcel for somebody with no booking at all is wrongly offered a student **0.02%** of the time. At a tolerance of one, the two-digit typo is found only 15% of the time. At three, the false offer rises to 0.35% and keeps climbing.

**The tolerance is not what makes this safe — the count is.** That simulation drew subscriber digits uniformly, and real numbers do not arrive that way: students buy SIMs in batches, so `…382` and `…383` can sit in one cabinet in a way the model never produces. So the rule that matters is **more than one candidate means no candidate**. The screen never offers a choice between two people, at any distance. It asks for the name instead.

**Why the shipper types the name rather than reading a list.** `maskName` keeps a family name, a middle initial and one letter — and in Vietnam that is a small set. `Nguyễn Văn Phong`, `Nguyễn Văn Phúc` and `Nguyễn Văn Phương` all mask to `Nguyễn V. P***`. Simulated over twenty live bookings, two masks are identical **56%** of the time, so a list would fail at its job in most full cabinets, and a shipper choosing between two identical rows is a coin flip that ends with a parcel in a stranger's reserved box — which that stranger can open, because the box really is theirs.

Typing inverts it. The server holds the **full** names and can tell Phong from Phúc; the screen shows a list to nobody. It is also strictly less leaky: a list hands a stranger twenty names at once, while a name check answers yes or no to one guess, only after a plausible number was typed first, and rate-limited. Neither rung ever names a person with no parcel on the way, and **no phone number appears anywhere in a response body** — rule 6 in [architecture.md](architecture.md) is about what crosses the wire, not what is drawn.

### How sure the server has to be

A door is the most expensive thing this system does, so what it takes to open one is written down rather than left to a screen.

| Number | Name typed | Confidence | What happens |
| --- | --- | --- | --- |
| Exact | not asked for | **Certain** | Proceed |
| Within two, one candidate | matches that candidate | **High** | Proceed, **and tell the receiver their booked number looks wrong** |
| Within two, one candidate | not given, or does not match | Unsure | Ask for the name. Do not proceed on the number alone |
| Within two, two or more candidates | matches exactly one of them | **High** | Proceed, and tell the receiver |
| Anything | matches none, or more than one | None | Rung E. ABO |

**"Proceed" never means a door opens by itself.** It means the screen stops asking the shipper for more and shows him the masked name and the box to confirm. The safety rule in [phase 3](../roadmap/phase-3-shipper-drop.md) is unchanged: a door opens only after the shipper confirms, never on a number alone.

**A name is compared with the accents and the case taken off**, and on the whole name, not a part of it. `NGUYEN VAN PHONG`, `Nguyen Van Phong` and `Nguyễn Văn Phong` are one name. `Nguyễn Văn Phúc` is not.

**Endpoint 11 prefers the booking's box.** When the receiver has a live booking, the drop goes into the door they reserved, and `claimFree` is not consulted. Without a booking it behaves exactly as before. A reserved box is never handed to a walk-up drop for somebody else, which is the whole point of reserving it.

**The shipper confirms the box number, not only the person.** Endpoint 11 answers with a box, and the screen shows it as a panel the shipper has to accept before the door moves:

> **Box 07.** Nguyễn V. A***. Put the parcel in box 07 and close it.
> `[ Open box 07 ]`   `[ Not right ]`

Two confirmations, and they are not the same question. The first is *have I got the right person*; the second is *am I about to walk to the right door*. A cabinet of twenty doors in four rows is easy to misread at arm's length with a parcel under one arm, and a parcel in the wrong open box is a parcel that the wrong student collects. The panel repeats the number in the sentence as well as on the button so a glance at either one is enough. **Not right** returns to the number, opens nothing, and releases the box.

This is the same shape as the receiver's confirm panel at endpoint 25, and for the same reason: the two moments in this system where a person types or reads a number that nothing else will check are the booking and the drop.

### Telling the receiver when the number was wrong

A mismatch resolved is still a mismatch, and the person who mistyped their number has no other way to find out. Whenever a drop completes at rung C or D, or fails to rung E, the server tells the receiver by push and by email — the channels in [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md), never SMS.

| What happened | What the receiver is told |
| --- | --- |
| Rung C or D — resolved, parcel in the box | "Your parcel is in box 07. **The courier's label said `…382` and you booked `…328`** — tap to fix your number, or the next one may not find you." |
| Rung E — went to ABO | "A courier could not find you and left your parcel at **ABO**, by the back gate. The number on the label was `…382`; yours is `…328`." |

**The notice names the digits, because a person cannot correct a number they are only told is wrong.** It goes to the account that owns the booking and nowhere else, so it reveals nothing to anybody who was not already the intended receiver. Endpoint 29 is the tap that fixes it.

**A command says why the door is opening, and the server checks the answer anyway.** Endpoint 12 needs `drop` or `collect` and they do opposite things — a collect closing writes the parcel off and frees the box, a drop closing does neither. Until 2026-08-22 endpoint 22 handed the hardware only `open`, so an ESP32 filling that field in had to guess, and a wrong guess either books a collection that never happened or loses one that did (BUG-009). Endpoint 22 now carries `purpose`, which is **additive** — firmware built against the older shape keeps working.

The server does not trust it. A parcel it opened for a collect is sitting in `opening` at that box and nothing else is, so it works the purpose out from its own state and uses that. A caller whose claim disagrees is logged and overruled. Both halves matter: the hardware should not have to guess, and a field on a request that decides whether a parcel is written off should not be the last word.

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
| `PHONE_NOT_REGISTERED` | Neither the number nor the name found exactly one person | Cabinet | "No match here. Call them, and leave the parcel at ABO by the back gate." |
| `GOOGLE_DOMAIN` | The Google account is not on a VGU domain, or carries no `hd` | App | "Use your VGU account — the one ending vgu.edu.vn." |
| `PHONE_IN_USE` | That number is already booked to another account | App | "That number is already in use. Check the digits." |
| `BOOKING_EXISTS` | This account already has a live booking | App | "You already have a box booked. Cancel it first." |
| `NO_FREE_BOX` | No free box of that size | Cabinet | "No box this size. Try a smaller one." |
| `BOX_FAULTY` | Box faulty or offline | Both | "That box is out of order. Staff know." |
| `BOX_ALREADY_FULL` | A box thought to be free is occupied | Cabinet | "That box is not empty. Tell staff." |
| `SESSION_EXPIRED` | QR session code expired | App | "Code expired. Scan again." |
| `NO_PARCEL_HERE` | No parcel for you at this cabinet | App | "Nothing waiting here." |
| `TOKEN_EXPIRED` | Token expired | App | *(send to register, no message)* |
| ~~`WRONG_CODE`~~ | ~~Wrong one-time code, at register~~ | — | **Retired 2026-09-02.** There is no one-time code. Not reused |
| `CODE_REJECTED` | Wrong, used, or expired pickup code | Cabinet | "That code did not work. Scan with the app instead." |
| `BOX_LOCKED_OUT` | Too many wrong tries | Cabinet | "Too many tries. Unlocks at 14:35." |

**`PHONE_INVALID` survives the removal of the code, and matters more than it did.** It means the number is not a Vietnamese mobile — `Phone.normalise` accepts `+84908619328`, `0908619328` and `908619328`, stores one form, and refuses a landline or a service range. With no code being sent, that check is the **only** automatic guard standing between a typo and a parcel handed to the wrong student, which is why it gets its first tests in the shipped server at task **P2-15**.

**`RATE_LIMITED` and `SEND_FAILED` are retired with the code path.** Both described an SMS provider that no longer exists in this system.

**Endpoints 20 and 21 add three more.** `PASSWORD_TOO_SHORT` and `PASSWORD_TOO_LONG` are the only rules on a password, and they come back from endpoint 20 only. `WRONG_PASSWORD` is the single answer endpoint 21 ever gives: it covers a wrong password, a locked-out account, and a phone number nobody has registered, on purpose. Telling them apart would answer two questions a sign-in screen must not answer — whether that number is a user here, and whether the lockout has started. It is the same rule `CODE_REJECTED` follows at the keypad, and the rule the retired `WRONG_CODE` followed before it.

**Endpoint 19 adds three more.** `GOOGLE_INVALID` covers every way an ID token can be wrong, in one code and on purpose: telling a caller *which* check failed helps only somebody probing it. `GOOGLE_OFF` means no client id is configured on that server, so the app must hide the Google button rather than show one that cannot work. `GOOGLE_DOMAIN` is the one exception to the single-answer rule, and deliberately: a student signing in with a personal Gmail has made an ordinary mistake and needs to be told which account to use, and the fact that this system only serves VGU is not a secret — it is written on the cabinet.

**`PHONE_REQUIRED` changed meaning on 2026-09-02.** It used to be the cue to ask for a phone number and a one-time code. There is no code, and the account already exists by the time it can be returned — so it is now the cue to send the person to the booking screen, endpoint 25. It is still not a failure.

**`CODE_REJECTED` covers three different failures on purpose.** Wrong, already used, and expired all return the same code and the same words. Splitting them would let somebody at the keypad work out which codes are real.

**A wrong typed code must not say whether that code exists.** "Not right" and "not right yet" are the same message. Anything else lets somebody at the keypad work out which codes are real.

## What is still open

Three of the four questions on this list were addressed to the Server team. Two of them are now ours to answer and are answered; the rest are below.

**Answered, because we build the server now:**

- *How does a cabinet get its key, and how is it replaced if one is stolen?* The server issues it once, at `cabinet add`, and keeps only a hash. `cabinet rotate` replaces it and kills the old one. What remains of **P0-05** is the hardware half — what a person physically does at a cabinet — and that is in [cabinet-firmware.md](cabinet-firmware.md).
- *Confirm the masked-name format.* Ours to decide, and decided: `maskName` in `Phone.kt` writes `Nguyễn V. A***`. **P0-08** is answered.

**Still open, and now ours to decide:**

1. **Endpoints 16, 17, 20 and 21 are written and not built.** Offline pickup, reconciliation and passwords. Nothing depends on them yet and none is on the path to a working pickup.
2. **Endpoints 25 to 29 are written and not built**, and endpoint 19 is built to the old shape. Tasks **P2-10** to **P2-14** and **P3-08** to **P3-11**.
3. **What happens when every box is booked and none is full?** Twenty live bookings fill a twenty-box cabinet with nothing inside it. The 24-hour expiry and the one-per-account limit are what bound it, and a walk-up drop then falls to rung E and goes to ABO. If it bites, the answer written down in [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md) is to stop holding a specific door and hold only the claim. Nobody has seen it happen yet, because nobody has used this yet.

**Answered on 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md):**

- *What does endpoint 10 return when two people share one phone number?* They cannot. The number is claimed at endpoint 25 and the second claim is refused with `PHONE_IN_USE`, naming nobody. The schema kept `UNIQUE` and that is now a chosen answer rather than an accident of it. **P3-03** is unblocked.

**Still open for the hardware team:** the three questions at the end of [cabinet-firmware.md](cabinet-firmware.md) — how a key reaches a board, how long a latch needs, and what happens on a power cut mid-open.

Everything else in this file is a number, and every number lives in the settings file. Want a different value? Change the setting. Endpoint 15 is built, so that reaches an installed phone; **P1-08** is the proof of it on a real one.
