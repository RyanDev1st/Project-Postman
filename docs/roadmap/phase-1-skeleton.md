# Phase 1 — Skeleton

**Goal:** an empty app runs on the real Android phone, an empty cabinet screen runs in a browser, and both reach the real server.

**Progress: 3 / 9.**  ·  **OPEN — P1-01 is built and waiting on a phone**

No feature in this phase. Only proof that the pipes exist.

Phase 0 no longer gates this phase — [ADR 0009](../adr/0009-start-phase-1-early.md). Two Verify lines here changed with it: **P1-03** proves the cabinet screen in a browser at two sizes, because there is no cabinet and no screen spec. Running on the real cabinet is **P8-03**, and that still names real hardware.

## Tasks

- [ ] **🟡 DOING — P1-01** — Empty app runs on the real Android phone
      - Owner: Team · Needs: P0-03, P0-09, P0-15 · Blocks: P1-04
      - Verify: the app opens on the listed Android phone and shows one screen with its version number
      - Notes: **the app is built. `app-debug.apk` exists and reads back as `vn.edu.vgu.smartlocker` v0.1.0, minSdk 24, targetSdk 36.** What is left is the half that needs a person: plug the Vivo in with USB debugging on, install it, and look at the screen. Nothing else blocks it. Build it again with `gradlew assembleDebug`; install with `adb install -r src/app/build/outputs/apk/debug/app-debug.apk`
      - Toolchain: Android Studio `A:\Android Studio`, SDK `A:\Android\Sdk` (`ANDROID_HOME`), Gradle `A:\gradle`, caches `A:\gradle-home`. **None of it is on C:**, so a guide assuming the default path points at nothing

- [ ] **⏸️ LATER — P1-02** — Empty app runs on the real iPhone
      - Owner: _unassigned_ · Needs: P0-03, P0-09 · Blocks: —
      - Verify: the app opens on the listed iPhone and shows one screen with its version number
      - Notes: deferred by [ADR 0007](../adr/0007-android-first.md) — Android MVP first. Needs a Mac and an Apple developer account, which is now the rest of the team's item. Do not let this hold up P1-04

- [x] **P1-03** — Empty cabinet screen runs in a browser
      - Owner: Team · Needs: P0-01, P0-14 · Blocks: P1-06, P8-03
      - Verify: the page opens full-screen in a browser and shows one screen with its version number, readable at **1280 × 800 and at 1920 × 1080** without a sideways scrollbar
      - Notes: was *"on the real cabinet"*. There is no cabinet, and P0-16 is deferred — [ADR 0009](../adr/0009-start-phase-1-early.md). The two sizes stand in for a screen spec nobody can give us yet, so the layout is proved to scale before anything is built on it. **Running it on the real cabinet is now P8-03**, and that Verify still names real hardware
      - Done: 2026-08-06 — Built the first cabinet screen and opened it in Chrome at both sizes the task asks for. It shows the locker name in English and Vietnamese, says plainly that it is not in service yet, and prints its version — v0.1.0 — along the bottom. Nothing was cut off and nothing needed scrolling sideways. Also tried it small, at 800 by 480, in case the real cabinet screen turns out to be a little tablet: everything still fit and stayed readable. The screen says "Cabinet not set" because no cabinet exists to name it, which is honest rather than broken. It does not talk to the server yet — that is P1-06.

- [x] **P1-04** — One network file in the app. Every call goes through it
      - Owner: Team · Needs: P1-01 · Blocks: P1-05
      - Verify: a search of the app source finds no HTTP call outside that one file
      - Notes: rule 1 in `architecture.md`. Doing this later means touching every screen. The search is `python scripts/checknet.py`, which also carries the P1-07 checks. No new libraries: Android already has everything this needed
      - Done: 2026-08-07 — Built the part of the app that talks to the server, and made it the only part that can. Every message the app will ever send now goes out through a single file, so the rules about it are decided once instead of once per screen: it refuses to talk over an unencrypted connection, it gives up rather than hanging forever, and it never writes anybody's login into a log. Wrote a checker that reads the whole app and fails if any other file tries to reach the network on its own — then deliberately added a bad file to make sure the checker actually caught it, which it did, before deleting it. Your login is kept in the locked-away store the phone provides, not as plain text. The app also carries our list of guessed numbers inside it now, copied straight from the one file we keep them in, so the two can never disagree. The app still talks to nobody, because the Server team has not given us an address yet — that is P1-05.

- [ ] **🔴 BLOCKED — P1-05** — The app reaches the real server over HTTPS
      - Owner: _unassigned_ · Needs: P1-04, P0-04 · Blocks: P1-07, P1-08, P2-01
      - Verify: the app calls one real endpoint and the server log shows the request arriving
      - Notes: **the app's half is built** — P1-04. It is blocked on there being a server to call: `server_base_url` in `config/settings.json` is empty and stays empty until P0-04 is sent and answered. P0-04 was added to Needs on 2026-08-07, because the dependency was always real and was not written down

- [ ] **🔴 BLOCKED — P1-06** — The cabinet reaches the real server with its key
      - Owner: _unassigned_ · Needs: P1-03, P0-04 · Blocks: P1-07, P3-01, P5-01
      - Verify: the cabinet calls one real endpoint and the server log shows it, identified as that cabinet
      - Notes: P0-05 was dropped from the Needs by [ADR 0009](../adr/0009-start-phase-1-early.md) — the key **type** is deferred, so this uses a simple test key we issue ourselves. The key is placed on the device, never built into the code, and the real design lands before any cabinet leaves a desk
      - Notes: **the cabinet's half is built** — `src/cabinet/net.js` is its one door, and `config.example.js` shows where the key goes. `python scripts/checknet.py` proves nothing else in `src/cabinet/` touches the network. What is missing is a server: `server_base_url` is blank, because the contract has not been sent (P0-04). Blocked on the Server team, not on us

- [ ] **P1-07** — No key, token or address inside either build
      - Owner: _unassigned_ · Needs: P1-05, P1-06 · Blocks: P8-07
      - Verify: a search of both source trees for keys and addresses returns nothing, and `.gitignore` covers the config files
      - Notes: the cabinet key is placed on the device, never built into the code

- [ ] **P1-08** — A changed setting reaches a phone without a new release
      - Owner: _unassigned_ · Needs: P0-15, P1-05 · Blocks: —
      - Verify: change one number in `config/settings.json`, do **not** rebuild the app, and the phone behaves by the new number
      - Notes: design is in [config/README.md](../../config/README.md). The server side is endpoint 15 in `api-contract.md`, proposed 2026-08-06 — it goes to the Server team with P0-04. Until this is ticked, every guessed number is frozen at whatever shipped

- [x] **P1-09** — Name every screen in the app, and what it must never show
      - Owner: Team · Needs: P1-01 · Blocks: P2-01, P4-04, P5-02
      - Verify: `docs/reference/app-screens.md` names every screen, says what leads where, and says what each must never show — **and** every named screen exists as an empty Compose screen the app builds with
      - Notes: wireframes and flow only. Colour, type, icons and motion are deliberately left out — the API shape is not agreed yet, and styling a screen the contract may still reshape is work done twice. **The home screen shows one parcel full-screen**, not a list; a list appears only with two or more. Decided with the team on 2026-08-06
      - Done: 2026-08-06 — Drew the whole app on paper first: seven screens, what each one is for, which button leads where, and — the part that matters most — what each screen must never show. A stranger can always see over your shoulder, so the app never puts a full name or somebody else's phone number on screen, and even your own number is masked. Then built all seven as real screens you can tap through, with made-up parcels and no server behind them. The app compiles with no warnings. **Nobody has seen these on a real phone yet** — that needs the Vivo, the same as P1-01. What we have is the shape agreed before anyone spends a week building the wrong one.

## Exit check

- [ ] All nine tasks ticked
- [ ] The app runs on a real Android *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The cabinet screen runs on the real cabinet
- [ ] Both front-ends appear in the server log
- [ ] Counts updated in [README.md](README.md)
