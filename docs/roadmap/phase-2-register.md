# Phase 2 — Register

**Goal:** a receiver registers with a phone number, gets a token, and stays logged in.

**Progress: 5 / 17.** Ticked, that is - more than three are done: P2-01, P2-03, P2-04 and P2-05 all work and were driven end-to-end on an emulator on 2026-08-21. Every `Verify` in this phase but one says *a real Android phone*, and an emulator is not one, so those stay unticked. The exception is P2-07, whose `Verify` asks only that the token is cleared and that nobody is sent round a loop - both of which an emulator can show, and did. The gap is one session with Ryan's phone, not more building. P2-02 was the real blocker and it is now moot: [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md) removed the one-time code, so no account waits on SpeedSMS again. **Four tasks - P2-01, P2-02, P2-03 and P2-06 - are marked `⛔ SUPERSEDED`.** They keep their IDs, are never renumbered, and can never be ticked; P2-14 is what takes their code out. Eight new tasks, P2-10 to P2-17, carry the work 0026 created, and P2-10, P2-11, P2-12 and P2-15 are ticked. `docs/findings/2026-08-21-otp-channel-choice.md` is now history rather than a plan.

The receiver must be registered and logged in before a parcel is ever dropped. Everything later depends on this.

## Tasks

- [ ] **P2-01** — ⛔ SUPERSEDED — Screen: type a phone number, ask for a one-time code
      - Owner: _unassigned_ · Needs: P1-05 · Blocks: P2-02
      - Verify: a real phone number is entered on the real Android phone and the server log shows the request
      - Superseded 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md): there is no one-time code any more. The way in is Google on a VGU domain, and the number is claimed inside a booking. Kept, never renumbered, and it can never be ticked. Replaced by P2-13.
      - Notes: P0-04 was dropped from the Needs by [ADR 0009](../adr/0009-start-phase-1-early.md) — we build against our own proposed contract, per [ADR 0005](../adr/0005-we-propose-they-object.md). **The shape of endpoints 1 and 2 must be agreed with the Server team before this is built**, because a shape change here rebuilds the screen. The field names and the path can change cheaply; the call existing at all cannot
      - Status 2026-08-21: **built and working, not ticked.** `Verify` says a real Android phone; this was the emulator `parity` (Android 16) against the real server over TLS, driven through the real screens. Typed `+84 912 340 001`, the server logged the request and generated a code. What is left is one run on Ryan's phone, not code.

- [ ] **P2-02** — ⛔ SUPERSEDED — The one-time code arrives on the phone
      - Owner: _unassigned_ · Needs: P2-01 · Blocks: P2-03
      - Verify: a real code arrives on a real phone within the agreed time
      - Superseded 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md): there is no one-time code any more. The way in is Google on a VGU domain, and the number is claimed inside a booking. Kept, never renumbered, and it can never be ticked. Replaced by P2-13.
      - This was the phase blocker. It is now moot: no account ever waits on SpeedSMS again.
      - Status 2026-08-21: **blocked, and not on us.** The code reaches a terminal, not a phone. The SpeedSMS request was wrong in three ways and all three are fixed (BUG-016), but the account has no registered sender, so the provider refuses every send. That is a job in SpeedSMS's dashboard. Free alternative if money is the objection: `sms_type` 5, their Android gateway app, which sends from Ryan's own SIM.

- [ ] **P2-03** — ⛔ SUPERSEDED — Send the code back and get a token
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: P2-04, P2-06
      - Verify: a real phone number and its real code reach the empty parcel list screen
      - Superseded 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md): there is no one-time code any more. The way in is Google on a VGU domain, and the number is claimed inside a booking. Kept, never renumbered, and it can never be ticked. Replaced by P2-13.
      - Status 2026-08-21: **built and working, not ticked.** Emulator `parity` (Android 16), real server over TLS, real screens: typed the code, reached Home, and the server issued a token. Same as P2-01 - it needs a real phone to close, not work.

- [ ] **P2-04** — Store the token in the phone's secure store
      - Owner: _unassigned_ · Needs: P2-03 · Blocks: P2-05, P5-02
      - Verify: the token is not in plain text anywhere on the device, and never appears in a log line
      - Notes: not a plain file, not app settings. The platform secure store
      - Status 2026-08-21: **verified, and this one was tested properly.** Emulator `parity` (Android 16), real server over TLS, real screens. The stored value was read off the device as root: `shared_prefs/...session.xml` holds a 71-byte blob (12-byte IV, 43-byte ciphertext, 16-byte GCM tag), and hashing that string does not match the server's token hash - so the file does not contain the token. The key is in AndroidKeyStore, so the file alone is useless. Nothing token-shaped in logcat. Still wants one run on a real phone to tick.

- [ ] **P2-05** — Stay logged in after the app is closed and reopened
      - Owner: _unassigned_ · Needs: P2-04 · Blocks: P2-07, P4-02, P4-04
      - Verify: close the app fully, reopen it, and it does not ask to register again
      - Status 2026-08-21: **built and working, not ticked.** Emulator `parity` (Android 16), real server over TLS, real screens: `am force-stop`, relaunch, and the app opened straight to Home without asking to register. Needs a real phone to close.

- [ ] **P2-06** — ⛔ SUPERSEDED — Plain messages for a wrong code, an expired code, and too many tries
      - Owner: _unassigned_ · Needs: P2-03 · Blocks: —
      - Verify: each of the three cases shows its own plain sentence on a real phone. No code shown raw
      - Superseded 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md): there is no one-time code any more. The way in is Google on a VGU domain, and the number is claimed inside a booking. Kept, never renumbered, and it can never be ticked. Replaced by P3-09, P3-10 and P3-11.
      - Status 2026-08-21: **half done.** Two of the sentences were missing entirely - a cooldown and a failed send both showed "Something went wrong. Try again.", which invites the one action that cannot work (BUG-018, fixed, both languages). The three code cases in this task's `Verify` - wrong, expired, too many tries - have still not been walked on a screen.

- [x] **P2-07** — Log out, and handle an expired token
      - Owner: Claude · Needs: P2-05 · Blocks: —
      - Verify: log out clears the token; an expired token sends the user back to register once, with no loop
      - Done: 2026-08-22 — Signed in on the emulator with a real account and a real code, then tapped **Log out** in Settings. The app went back to the sign-in screen and the phone's token store was empty afterwards — nothing left behind. Then the other half: signed in again, deleted that account's token on the server so the phone was holding a dead one, and reopened the app. It landed on sign-in **once**, saying *"You were signed out. Enter your number to sign in again."*, threw the dead token away, and sat still — checked again at six, twelve and eighteen seconds, same screen every time. Typing the number again signed straight back in and reached Home. So there is no loop, and there cannot be one: the phone has nothing left to send.
      - Notes: two bugs came out of the walk. **BUG-020** — the Log out row is drawn underneath the bottom navigation bar, so the first tap hits the Cabinet tab instead; it works after one scroll, which is how it was tested. **BUG-019** — unrelated to this task and far worse: every second call the app made was failing. Fixed and proven in the same session.

- [ ] **P2-08** — Sign in with Google, on top of the phone number
      - Owner: _unassigned_ · Needs: P2-05 · Blocks: —
      - Verify: on a real Android phone, tapping "Continue with Google" on an account that has already registered by phone reaches the parcel list without a code being typed; and a Google account that has never registered is asked for a phone number first
      - Notes: decided in [ADR 0011](../adr/0011-google-sign-in-no-passwords.md). Endpoint 19 in [api-contract.md](../reference/api-contract.md). No passwords are added by this task, on purpose
      - Changed 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md): **Google now makes the account**, which 0011 forbade. Built 2026-09-03 with P2-11: `PHONE_REQUIRED` is **gone**, not repurposed. A first sign-in creates the receiver with no number at all, and the number is claimed later at the booking - so there is no cue to send back, and an account without a number is a normal state rather than an error. The domain check is P2-11
      - Notes: **the server half is done and live as of 2026-08-22. The app half is blocked on an OAuth client id, which only Ryan can create.** `GoogleTokens` and `GoogleCerts` were ported out of `src/otp-server/`, which is not a Gradle module and was never built, so the shipping server had no answer for endpoint 19 at all. It has one now: `/auth/google` signs in a linked Google account, links one when a receiver token rides along, and answers `PHONE_REQUIRED` otherwise - which is a cue, not a failure. Google can still never **make** an account, because a Google account has no phone number and the shipper finds people by number
      - Notes: the ID token is checked here rather than at Google's `tokeninfo`, so the login path carries no network call to somebody else's rate limit. Thirteen tests attack the verifier with tokens minted in the test itself - `alg: none`, RS256 swapped for HMAC, a token minted for another app, another issuer, expired, signed by somebody else, claims edited after signing. Five more, on a real database file, hold the rule that decides whose parcels a Google sign-in reaches: **one Google account reaches one receiver and never two**, kept by a unique index. Schema migration 6
      - Notes: probed on the running server over TLS. With no `GOOGLE_CLIENT_ID` the endpoint answers `501 GOOGLE_OFF`, so the app can hide the button rather than show one that cannot work. With one set, four different bad tokens all came back `400 GOOGLE_INVALID` - one answer for every way a token can be wrong, so the verifier cannot be probed for which check failed

- [ ] **P2-09** — Set a password, and sign in with one
      - Owner: _unassigned_ · Needs: P2-05 · Blocks: —
      - Verify: on a real Android phone, set a password after registering by code, close the app fully, sign in with the phone number and that password, and reach the parcel list. Then type it wrong five times and confirm the right password stops working until fifteen minutes have passed
      - Notes: decided in [ADR 0012](../adr/0012-passwords-on-a-phone-account.md), which reverses the no-passwords half of [ADR 0011](../adr/0011-google-sign-in-no-passwords.md). Endpoints 20 and 21. **No email anywhere, and no reset screen** — forgetting a password is the one-time code, then set a new one, which is why the set screen is reachable only from a live session
      - Notes: **built on both sides and walked end to end on 2026-08-22; not ticked, because `Verify` says *a real Android phone*.** The server half was ported out of `src/otp-server/`, which is not a Gradle module and was never built - Argon2id through BouncyCastle, schema migration 5, eight tests. The app half is `SetPasswordScreen` from Settings, `PasswordSignInScreen` from the sign-in screen, and a shared `PasswordField`.
        Walked on the `parity` emulator against the real server over TLS, through the real screens. Set `vgu-locker-2026` on a real account: the server stored `argon2id$19456$2$1$…` and the password itself appears nowhere. Logged out, tapped *Sign in with a password instead*, typed the number and the password, and reached the parcel list with box 07 on it. Then five wrong guesses through the same screen: the fifth set a lock 14.9 minutes ahead, and the **correct** password straight after was still refused - which is the half that matters.
        Changed 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md): the reset path in the note above was *sign in with a code, then set a new one*. There is no code now, so it is *sign in with Google, then set a new one*. Same shape, nothing new to build.

        Two properties were checked rather than assumed. The screen says the same sentence for a wrong password, an unknown number and a locked account, so it never answers *is this number registered*; and it never says *locked*, because being told the lockout started is being told the password was right. And the counter survives a power cut: `PRAGMA synchronous` is `2`, FULL, held by `DurabilityTest` so a driver upgrade cannot quietly take it away

- [x] **P2-10** — Rewrite the contract for a booked box and a Google-made account
      - Owner: Claude · Needs: — · Blocks: P2-11, P2-12, P2-13, P3-08
      - Verify: `api-contract.md` has no endpoint 1 or 2, endpoint 19 makes an account, and three booking endpoints are written with every field named. Both front-ends can be built from it without asking a question
      - Notes: [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md). **The contract changes first.** The cabinet screen and the app both move off it, and a shape change after they are built rebuilds both
      - Done: 2026-09-02 — Rewrote the contract so a person signs in with their VGU Google account and books a box, and no text message is ever sent to anybody. Read the whole file back afterwards: the two one-time-code calls are struck out and nothing new takes their numbers, signing in with Google now creates the account, and there are three new calls for booking a box plus one that tells the cabinet who is expecting a parcel. Two holes were found by reading it as somebody who had to build from it - the booking never said which cabinet it was for, and the two password calls had lost their table heading and would have printed as a row of broken bars - and both were fixed. Every link in the file opens, and all eighteen tables draw.
      - Notes: the shipper's screen is written down as a ladder of five rungs, A to E, ending at ABO. Lettered rather than numbered because every other number at the start of a row in this file is an endpoint

- [x] **P2-11** — The `hd` allow-list, and the dot that makes it safe
      - Owner: Claude · Needs: P2-10 · Blocks: P2-13
      - Verify: a token carrying `hd` of `vgu.edu.vn` signs in, and so does one of `student.vgu.edu.vn`. `notvgu.edu.vn`, `vgu.edu.vn.example.com` and a token with no `hd` at all are each refused. Tests mint their own tokens, the way `GoogleTokensTest` already does
      - Notes: `hd == "vgu.edu.vn" || hd.endsWith(".vgu.edu.vn")`. **The leading dot is the check** - without it `notvgu.edu.vn` is accepted. Domains come from `google_allowed_domains` in `config/settings.json`, so a new subdomain is a text edit. The claim read is `hd`, never the email address, which can be an alias
      - Done: 2026-09-03 — The server now only lets in people whose Google account belongs to the university. A staff sign-in on `vgu.edu.vn` and a student one on `student.vgu.edu.vn` both go through. A personal Gmail is turned away, and so is `notvgu.edu.vn` — a real address anybody could buy for a few dollars, and the one that a careless version of this check would have let straight in. Eighteen tests now run against the sign-in check, five of them new and written for this. Then the check was deliberately broken to be sure the tests were not just agreeing with everything: with the dot taken out, `notvgu.edu.vn` was accepted and the test went red. Put back, green again.
      - Notes: a student who taps the button with their personal Gmail is told **which** account to use — the one refusal in this file that says why. Every other way a sign-in can fail still gives one flat answer, so nobody can poke at it to learn which part they got past

- [x] **P2-12** — Schema: a number that may be absent, and a table of bookings
      - Owner: Claude · Needs: P2-10 · Blocks: P2-13
      - Verify: the migration runs on a copy of the live database file, and a second start on the same file does not try to add the column again. An account with no number is legal; two accounts with the same number are not
      - Notes: `receivers.phone` becomes nullable and unique only when set. New `bookings` table - one live booking to a receiver, the box it holds, and an expiry 24 hours out
      - Done: 2026-09-03 — Took a copy of the real database the server has been running on and upgraded it with the real server code. Everything that was in it is still in it: all 34 people, all 27 parcels, all 101 log entries, both cabinets. An account is now allowed to exist with no phone number yet, which is what a brand-new Google sign-in is, and the new table that holds a booked box is there. Started the server a second time on the same file and it changed nothing, which is the thing that proves the upgrade only runs once.
      - Notes: the upgrade has to **rebuild** the people table, because the phone number column can only stop being compulsory that way. Doing that with the database's own rules switched on would have deleted every login, device and parcel in the file — quietly, and the server would have started afterwards as if nothing was wrong. The rules are switched off around the rebuild and the file is checked afterwards. A test builds a database in the old shape, fills it, upgrades it, and fails if anything vanishes; with the guard removed on purpose, it goes red
      - Notes: **the upgrade found a real fault in the file — BUG-030.** 180 of its 199 logins belonged to accounts that had been deleted months ago, and each one still worked. They came from the load-testing script, which cleans up after itself over a connection where the database's own rules are off, so the logins it should have removed were left behind. The script is fixed, and the upgrade throws the leftovers away: 199 logins went to 19, which are the 19 that belong to somebody

- [ ] **P2-13** — Book a box, and read the number back before it is taken
      - Owner: _unassigned_ · Needs: P2-11, P2-12 · Blocks: P2-14, P2-17, P4-01
      - Verify: on a real Android phone, sign in with a `@student.vgu.edu.vn` account, book a box, and see the number echoed as `+84 908 619 328` on a second panel before it is accepted. `0908619328`, `908619328` and `+84908619328` all reach that same confirmed number. **Book a second time and the number is not asked for again.** A second booking while one is still live is refused
      - Notes: the panel shows the **stored** form, not the typed one. A panel that repeats the same shape somebody just typed is one the eye slides over, and a typo here is silent
      - Notes: **the number is typed once and kept on the account.** Later bookings send a cabinet and a size and nothing else. Every re-typing is another chance to introduce the very typo the cabinet's ladder exists to survive
      - Notes: 2026-09-03 — **the server half is built and checked**: booking, cancelling, reading a booking back, and the rule that the number is asked for once and then kept. Seventeen tests cover it and `scripts/checkserver.py` walks it end to end
      - Notes: 2026-09-03 — **the app now signs in with Google.** The button is on the sign-in screen, above the number field, and it posts a token Google signed to endpoint 19. It compiles, the client id reaches the build out of `/.env`, and five tests read the server's own list of refusals and fail if the app stops understanding one of them. Two things are still owed before this ticks: **the booking screen** — the box, the number typed once, and the panel that reads it back as `+84 908 619 328` — and a run on a **real phone**
      - Notes: 2026-09-03 — **run on an emulator, and it got further than expected.** The app was installed on `parity-gms`, its saved login cleared, and the button tapped. Google's own sign-in screen opened — which means Google Play Services **accepted the account id this build was given**, and the fear that no such account had ever been set up for this app is answered. It then asked to add a Google account, because that emulator has none signed in, and adding one needs somebody's real Google password, so the run stops there. What is still unknown is only the last step: whether Google hands back a token once a real account is picked. Pressing back returned to the sign-in screen with the button ready again and no error on it, which is what it is meant to do — closing a picker is an answer, not a fault
      - Notes: the number and the one-time code stay on the screen underneath until that run passes. Taking them out first would leave a build whose only way in does not work with nobody able to register at all. That is what P2-14 is for, and why it needs this task first

- [ ] **P2-14** — Take the one-time code out
      - Owner: _unassigned_ · Needs: P2-13 · Blocks: —
      - Verify: `Otp.kt`, `Sms.kt` and the SpeedSMS dependency are gone, `speedSmsToken` is out of `config/settings.json`, and the server starts and serves every remaining endpoint with no SMS provider configured at all
      - Notes: this is what closes P2-01, P2-02, P2-03 and P2-06. **After P2-13 works, not before** - the new way in has to exist before the old one is taken away

- [x] **P2-15** — First tests for `Phone.normalise` and `maskName`
      - Owner: Claude · Needs: — · Blocks: P2-13
      - Verify: the three accepted shapes all reach one stored form; a landline and a service range are refused; `Nguyễn Văn An` masks to `Nguyễn V. A***`; a one-word name and an empty one do not crash. Change the rule and a test goes red
      - Notes: both are untested in the shipped server today, though the `src/otp-server/` prototype has tests for them. With no code sent, `normalise` is the **only** automatic guard between a typo and a misdelivery
      - Done: 2026-09-03 — Eleven tests, and they found a real gap. The number check used to accept anything that was not a landline, which let through four leading digits no Vietnamese mobile has ever had — including `1900` and `1800`, the numbers a company prints on a poster. It now accepts the five that are real and refuses the rest. Every way a person writes their own number reaches one stored form: with the country code, without it, with the zero, without the zero, with spaces, dashes or brackets. The name check turns `Nguyễn Văn An` into `Nguyễn V. A***` and does not fall over on a one-word name or an empty one. Then both rules were changed on purpose to make sure the tests were not just agreeing with the code: loosening the number rule turned two tests red, and showing one more letter of the name turned four red.
      - Notes: one test exists only to keep a mistake buried. `Nguyễn Văn Phong`, `Nguyễn Văn Phúc` and `Nguyễn Văn Phương` all mask to the same thing, and that is the measurement that killed the masked list at the cabinet. If somebody ever puts the list back, this test says why they should not

- [ ] **P2-16** — Vietnamese is what the app speaks first
      - Owner: _unassigned_ · Needs: — · Blocks: —
      - Verify: on a real Android phone set to English, the app opens in **Vietnamese**, and the Language row switches it to English on the next frame with no restart
      - Notes: `values/` holds English today and `values-vi/` Vietnamese, so every non-Vietnamese phone gets English. Vietnamese moves to `values/`, English to `values-en/`. The switch itself already exists - [ADR 0015](../adr/0015-language-in-the-composition.md) built it
      - Notes: 2026-09-03 — **done and seen, but on an emulator, so not ticked.** All 133 sentences moved: Vietnamese is now what the app falls back to and English is the option beside it. On an emulator set to English the app opened in Vietnamese and the Language row turned it English on the next frame with the sheet still open and no restart. `StringsMatchTest` fails the build if either language is missing a sentence the other has, which matters more here than anywhere else — a sentence missing from the fallback is a crash and not a fallback. The Verify says a real phone, and this phase does not tick on an emulator
- [ ] **P2-17** — Fix a number that was typed wrong
      - Owner: _unassigned_ · Needs: P2-13 · Blocks: —
      - Verify: the notice saying the courier's label and the booked number disagree opens a screen that changes the number in one tap. A number another account already holds is refused, naming nobody. A booking that is already live keeps the old number
      - Notes: endpoint 29. This is the other half of the notice - somebody told their number is wrong needs a way to correct it, and a live booking must not move, because the parcel already on its way was addressed to the old number
      - Notes: 2026-09-03 — endpoint 29 is built and checked on the server, including the refusal when another account already holds that number, which names nobody. The **screen** that calls it is not built, and it belongs beside the notice at P4-06 that sends somebody to it
## Exit check

- [ ] All seventeen tasks ticked, less the four marked `⛔ SUPERSEDED`
- [ ] A real `@student.vgu.edu.vn` account signs in and books a box on a real Android *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The token is in the secure store and in no log line
- [ ] Every error case shows a plain sentence, in Vietnamese first
- [ ] Counts updated in [README.md](README.md)
