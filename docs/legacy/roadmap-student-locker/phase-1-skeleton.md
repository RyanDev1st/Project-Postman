# Phase 1 — Skeleton

**Goal:** an empty app runs on both real phones and reaches the real server. No features yet.

**Progress: 0 / 7.**

This phase proves the pipe works. Every later phase pushes data through this pipe.

## Tasks

- [ ] **P1-01** — Create the app project in the chosen tech and commit it
      - Owner: _unassigned_ · Needs: P0-01, P0-09 · Blocks: P1-02, P1-03, P1-07
      - Verify: `git clone` then one build command produces an app
      - Notes: follow the folder rules in `CLAUDE.md` from the first file

- [ ] **P1-02** — Build and run the empty app on a real Android phone
      - Owner: _unassigned_ · Needs: P1-01, P0-08 · Blocks: —
      - Verify: the app is installed on the Android device from P0-08 and opens

- [ ] **P1-03** — Build and run the empty app on a real iPhone
      - Owner: _unassigned_ · Needs: P1-01, P0-08 · Blocks: —
      - Verify: the app is installed on the iPhone from P0-08 and opens
      - Notes: needs a Mac and an Apple developer account. Start this early — it takes the longest to unblock

- [ ] **P1-04** — Write the one place in the app that talks to the server. Every request goes through it
      - Owner: _unassigned_ · Needs: P0-05, P1-01 · Blocks: P1-05, P2-02, P4-02
      - Verify: every network call in the repo goes through this one file. A grep proves it
      - Notes: one file. Later phases only add calls to it

- [ ] **P1-05** — Call one test endpoint on the real server and show the answer on screen
      - Owner: _unassigned_ · Needs: P1-04 · Blocks: P1-06
      - Verify: the real server's answer is visible on the phone screen
      - Notes: proves the pipe end to end

- [ ] **P1-06** — Move the server address out of the code into config: one for test, one for real
      - Owner: _unassigned_ · Needs: P1-05 · Blocks: P8-06
      - Verify: switching test ↔ real needs a config change only, no code edit
      - Notes: never a hard-coded address. Never a key inside the app code

- [ ] **P1-07** — Wire the empty screens together: login → point list → locker list → locker
      - Owner: _unassigned_ · Needs: P1-01 · Blocks: P2-01
      - Verify: a person can walk all four screens forward and back with no crash
      - Notes: empty screens. Navigation only

## Exit check

- [ ] All seven tasks ticked
- [ ] The app is installed and opens on both real devices from P0-08
- [ ] A real server answer is visible on both phones
- [ ] Counts updated in [README.md](README.md)
