# Phase 1 — Skeleton

**Goal:** an empty app runs on the real Android phone, an empty cabinet screen runs in a browser, and both reach the real server.

**Progress: 1 / 8.**  ·  **OPEN — P1-01 can start now**

No feature in this phase. Only proof that the pipes exist.

Phase 0 no longer gates this phase — [ADR 0009](../adr/0009-start-phase-1-early.md). Two Verify lines here changed with it: **P1-03** proves the cabinet screen in a browser at two sizes, because there is no cabinet and no screen spec. Running on the real cabinet is **P8-03**, and that still names real hardware.

## Tasks

- [ ] **P1-01** — Empty app runs on the real Android phone
      - Owner: _unassigned_ · Needs: P0-03, P0-09, P0-15 · Blocks: P1-04
      - Verify: the app opens on the listed Android phone and shows one screen with its version number
      - Notes: **every Need is ticked — this can start now.** P0-17 was dropped from the list by [ADR 0009](../adr/0009-start-phase-1-early.md); `minSdk` is 24, guessed. Set that in the build file when the project is created

- [ ] **⏸️ LATER — P1-02** — Empty app runs on the real iPhone
      - Owner: _unassigned_ · Needs: P0-03, P0-09 · Blocks: —
      - Verify: the app opens on the listed iPhone and shows one screen with its version number
      - Notes: deferred by [ADR 0007](../adr/0007-android-first.md) — Android MVP first. Needs a Mac and an Apple developer account, which is now the rest of the team's item. Do not let this hold up P1-04

- [x] **P1-03** — Empty cabinet screen runs in a browser
      - Owner: Team · Needs: P0-01, P0-14 · Blocks: P1-06, P8-03
      - Verify: the page opens full-screen in a browser and shows one screen with its version number, readable at **1280 × 800 and at 1920 × 1080** without a sideways scrollbar
      - Notes: was *"on the real cabinet"*. There is no cabinet, and P0-16 is deferred — [ADR 0009](../adr/0009-start-phase-1-early.md). The two sizes stand in for a screen spec nobody can give us yet, so the layout is proved to scale before anything is built on it. **Running it on the real cabinet is now P8-03**, and that Verify still names real hardware
      - Done: 2026-08-06 — Built the first cabinet screen and opened it in Chrome at both sizes the task asks for. It shows the locker name in English and Vietnamese, says plainly that it is not in service yet, and prints its version — v0.1.0 — along the bottom. Nothing was cut off and nothing needed scrolling sideways. Also tried it small, at 800 by 480, in case the real cabinet screen turns out to be a little tablet: everything still fit and stayed readable. The screen says "Cabinet not set" because no cabinet exists to name it, which is honest rather than broken. It does not talk to the server yet — that is P1-06.

- [ ] **P1-04** — One network file in the app. Every call goes through it
      - Owner: _unassigned_ · Needs: P1-01 · Blocks: P1-05
      - Verify: a search of the app source finds no HTTP call outside that one file
      - Notes: rule 1 in `architecture.md`. Doing this later means touching every screen

- [ ] **P1-05** — The app reaches the real server over HTTPS
      - Owner: _unassigned_ · Needs: P1-04 · Blocks: P1-07, P1-08, P2-01
      - Verify: the app calls one real endpoint and the server log shows the request arriving

- [ ] **P1-06** — The cabinet reaches the real server with its key
      - Owner: _unassigned_ · Needs: P1-03 · Blocks: P1-07, P3-01, P5-01
      - Verify: the cabinet calls one real endpoint and the server log shows it, identified as that cabinet
      - Notes: P0-05 was dropped from the Needs by [ADR 0009](../adr/0009-start-phase-1-early.md) — the key **type** is deferred, so this uses a simple test key we issue ourselves. The key is placed on the device, never built into the code, and the real design lands before any cabinet leaves a desk

- [ ] **P1-07** — No key, token or address inside either build
      - Owner: _unassigned_ · Needs: P1-05, P1-06 · Blocks: P8-07
      - Verify: a search of both source trees for keys and addresses returns nothing, and `.gitignore` covers the config files
      - Notes: the cabinet key is placed on the device, never built into the code

- [ ] **P1-08** — A changed setting reaches a phone without a new release
      - Owner: _unassigned_ · Needs: P0-15, P1-05 · Blocks: —
      - Verify: change one number in `config/settings.json`, do **not** rebuild the app, and the phone behaves by the new number
      - Notes: design is in [config/README.md](../../config/README.md). The server side is endpoint 15 in `api-contract.md`, proposed 2026-08-06 — it goes to the Server team with P0-04. Until this is ticked, every guessed number is frozen at whatever shipped

## Exit check

- [ ] All eight tasks ticked
- [ ] The app runs on a real Android *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The cabinet screen runs on the real cabinet
- [ ] Both front-ends appear in the server log
- [ ] Counts updated in [README.md](README.md)
