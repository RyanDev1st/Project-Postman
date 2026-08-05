# Phase 1 — Skeleton

**Goal:** an empty app runs on both real phones, an empty screen runs on the real cabinet, and both reach the real server.

**Progress: 0 / 7.**

No feature in this phase. Only proof that the pipes exist.

## Tasks

- [ ] **P1-01** — Empty app runs on the real Android phone
      - Owner: _unassigned_ · Needs: P0-03, P0-09 · Blocks: P1-04
      - Verify: the app opens on the listed Android phone and shows one screen with its version number

- [ ] **P1-02** — Empty app runs on the real iPhone
      - Owner: _unassigned_ · Needs: P0-03, P0-09 · Blocks: P1-04
      - Verify: the app opens on the listed iPhone and shows one screen with its version number
      - Notes: needs a Mac and an Apple developer account. Check this in P0-09, not here

- [ ] **P1-03** — Empty screen runs on the real cabinet
      - Owner: _unassigned_ · Needs: P0-01, P0-02, P0-03 · Blocks: P1-06
      - Verify: the cabinet screen shows one screen with its version number, at the cabinet, not on a desk

- [ ] **P1-04** — One network file in the app. Every call goes through it
      - Owner: _unassigned_ · Needs: P1-01, P1-02 · Blocks: P1-05
      - Verify: a search of the app source finds no HTTP call outside that one file
      - Notes: rule 1 in `architecture.md`. Doing this later means touching every screen

- [ ] **P1-05** — The app reaches the real server over HTTPS
      - Owner: _unassigned_ · Needs: P1-04 · Blocks: P1-07, P2-01
      - Verify: the app calls one real endpoint and the server log shows the request arriving

- [ ] **P1-06** — The cabinet reaches the real server with its key
      - Owner: _unassigned_ · Needs: P1-03, P0-05 · Blocks: P1-07, P3-01, P5-01
      - Verify: the cabinet calls one real endpoint and the server log shows it, identified as that cabinet

- [ ] **P1-07** — No key, token or address inside either build
      - Owner: _unassigned_ · Needs: P1-05, P1-06 · Blocks: P8-07
      - Verify: a search of both source trees for keys and addresses returns nothing, and `.gitignore` covers the config files
      - Notes: the cabinet key is placed on the device, never built into the code

## Exit check

- [ ] All seven tasks ticked
- [ ] The app runs on a real Android **and** a real iPhone
- [ ] The cabinet screen runs on the real cabinet
- [ ] Both front-ends appear in the server log
- [ ] Counts updated in [README.md](README.md)
