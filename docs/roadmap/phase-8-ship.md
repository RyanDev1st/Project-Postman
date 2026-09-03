# Phase 8 — Harden and ship

**Goal:** the system survives real phones, real networks and real people, and installs from a signed release build.

**Progress: 1 / 13.**

Device testing starts at the end of **every** phase, not only here. This phase is the final sweep.

## Tasks

- [ ] **P8-01** — Walk every screen on a real Android phone. Log every bug
      - Owner: _unassigned_ · Needs: P7-03 · Blocks: P8-04, P8-05, P8-08
      - Verify: every screen was opened on the real Android device, and the result is in the bug log
      - Notes: log to [bug-log.md](../reference/bug-log.md) as you go, not afterwards

- [ ] **⏸️ LATER — P8-02** — Walk every screen on a real iPhone. Log every bug
      - Owner: _unassigned_ · Needs: P7-03 · Blocks: —
      - Verify: every screen was opened on the real iPhone, and the result is in the bug log
      - Notes: deferred by [ADR 0007](../adr/0007-android-first.md). When iOS starts it gets its own phase, not a bolt-on here — that is how the deferred bugs stay a task instead of becoming a crisis

- [ ] **P8-03** — Walk every screen on the real cabinet. Log every bug
      - Owner: _unassigned_ · Needs: P7-03, P1-03 · Blocks: P8-04
      - Verify: every cabinet screen was used at the cabinet, and the result is in the bug log
      - Notes: **this is where the real cabinet becomes unavoidable.** [ADR 0009](../adr/0009-start-phase-1-early.md) let the screen be built in a browser; this task is the one that proves it on the real thing. Needs P0-16, which is deferred — so it cannot tick until a cabinet exists, and that is the point. First run on the real screen also settles assumption A-18

- [ ] **P8-04** — Fix every logged bug, then test it again on the device that found it
      - Owner: _unassigned_ · Needs: P8-01, P8-03 · Blocks: P8-09
      - Verify: every bug row is `fixed` **and** `re-tested`
      - Notes: a fix is not done until it is re-tested on the same device

- [ ] **P8-05** — Airplane-mode test on every screen
      - Owner: _unassigned_ · Needs: P8-01 · Blocks: P8-06
      - Verify: airplane mode on each screen shows a plain message. No crash, no blank screen, no false success

- [ ] **P8-06** — Slow-network test on every screen
      - Owner: _unassigned_ · Needs: P8-05 · Blocks: —
      - Verify: a 5-second-delay network never leaves the user with no feedback
      - Notes: slow is worse than off. Nothing may hang forever

- [x] **P8-07** — Check that no key, token or address sits in either build
      - Owner: Claude · Needs: P1-07 · Blocks: P8-09
      - Verify: a search of both source trees for keys and addresses returns nothing
      - Notes: anything found → replace it first, then remove it. Removing it alone is not enough
      - Done: 2026-09-03 — `python scripts/checksecrets.py` searches both front-ends and the server and finds nothing: no key, no token, no machine address, and the address the app is pointed at is blank in what is committed. It also opens the built app file and reads inside it, because the app is a zip and looking at the outside of it proves nothing. Every one of the four real secrets on this machine is searched for **by its actual value**, so this is not a guess about what a key looks like.
      - Notes: two holes were found and closed on the day it was ticked. The Google and reCAPTCHA settings moved into `/.env` when the one-time code was removed, and nothing was looking there. And searching for values you already hold finds nothing at all on a machine that holds none of them — a fresh clone, or a build server — so there is now a second pass for the **shape** of a key, which catches one nobody here has a copy of
      - Notes: proven able to fail. A file carrying a Google-shaped key and a private machine address was put in the cabinet screen and staged: two checks went red and named the file and the line. Removed, green again. The first attempt at that proof used a `scratch_` name, which git ignores, so the check could never have seen it — the probe was wrong, not the check

- [ ] **P8-08** — Small-screen and large-text test on the app
      - Owner: _unassigned_ · Needs: P8-01 · Blocks: —
      - Verify: at the largest system text size, every button is still reachable

- [ ] **P8-09** — Build the release version and install it on a clean phone
      - Owner: _unassigned_ · Needs: P8-04, P8-07 · Blocks: P8-10, P8-11
      - Verify: the release build installs and runs on a phone that never had the app
      - Notes: a clean phone finds bugs a developer phone hides

- [ ] **P8-10** — Write the install and rollback steps for whoever runs the release
      - Owner: _unassigned_ · Needs: P8-09 · Blocks: —
      - Verify: a person who did not build the app can follow the page and release it
      - Notes: one page. Include what to do if the release turns out bad, and how to reach a stuck cabinet

- [ ] **P8-11** — Get a Play Console account, verified, and sign the release with a key we keep
      - Owner: _unassigned_ · Needs: P8-09 · Blocks: P8-12
      - Verify: a signed `.aab` uploads to an internal testing track and installs on a phone from Play, not from a cable
      - Notes: Ryan decided on 2026-08-17 that the app is listed publicly — VGU has three to five thousand students and that is the population it has to survive. Four things, in this order: the $25 account, identity verification, an upload key, and Play App Signing.
        **The upload key is the one thing that cannot be replaced by trying again.** Lose it and no future version can be uploaded under this listing. `.gitignore` already refuses `*.jks` and `*.keystore`, so it must live somewhere a person owns and can find in a year.
        Two dates worth knowing: from 31 August 2026 a new app must target Android 16, and this app already does (`targetSdk = 36`). Developer verification starts enforcing on 30 September 2026 in four countries, none of them Vietnam, and goes worldwide in 2027

- [ ] **P8-12** — Pass the closed test and get the listing live
      - Owner: _unassigned_ · Needs: P8-11 · Blocks: —
      - Verify: the app installs from a public Play listing on a phone that was never a tester
      - Notes: **the long pole is fourteen days, not the code.** A personal account opened after 13 November 2023 must run a closed test with twelve opted-in testers for fourteen unbroken days before it may apply for production. Testers have to install and stay opted in; dropping below twelve resets the count. Twelve of three thousand students is the easy part — starting the clock late is not.
        Needs three things written before the fourteen days start: a privacy policy on a URL, a Data Safety form that matches what the app really collects (a phone number and a parcel history — say so), and store text that matches what the app really does. A form that disagrees with the app is the most common rejection there is

- [ ] **P8-13** — A release build has to trust the cabinet's own certificate
      - Owner: _unassigned_ · Needs: P8-09 · Blocks: P8-12
      - Verify: a **release** APK, installed on a phone with nothing added to its certificate store, registers and opens a box against the real server
      - Notes: decided by [ADR 0023](../adr/0023-not-on-the-public-internet.md) — the locker is not on the internet, so no public authority will ever sign its certificate. Only the **debug** build trusts certificates a person installed by hand; a release build trusts the system store and would fail every call, silently, on the first phone that is not a tester's. Ship the cabinet server's certificate in the app and trust that one. That also pins it: the app then refuses any certificate but ours, which is stronger than a public one, not weaker.
        Two things to get right. The certificate has to outlive the release — a one-year certificate makes every phone stop working on its birthday, so generate a long-lived one for the deployment and write down when it expires. And the app must trust our certificate **in addition to** the system store, or the Play install itself has nothing to check

## Exit check

- [ ] All thirteen tasks ticked
- [ ] The app installs from the public Play listing, on a phone that was never a tester
- [ ] The release build runs on a clean Android *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The cabinet screen runs from a fresh install
- [ ] The bug log has zero `open` rows
- [ ] Counts updated in [README.md](README.md)
