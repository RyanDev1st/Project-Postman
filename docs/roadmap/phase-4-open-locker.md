# Phase 4 — Open the locker

**Goal:** the user opens and locks a locker, and can see what they did before.

**Progress: 0 / 8.**

**This is the core phase.** An open request moves real metal. Treat every task here as safety work.

## Tasks

- [ ] **P4-01** — Build the locker screen with an open button and a lock button
      - Owner: _unassigned_ · Needs: P3-05 · Blocks: P4-02
      - Verify: both buttons appear and are reachable with one thumb

- [ ] **P4-02** — Send the open request to the API
      - Owner: _unassigned_ · Needs: P4-01, P1-04 · Blocks: P4-03, P4-04, P4-05, P4-08, P5-05, P7-06
      - Verify: the real locker opens

- [ ] **P4-03** — Send the lock request to the API
      - Owner: _unassigned_ · Needs: P4-02 · Blocks: —
      - Verify: the real locker locks

- [ ] **P4-04** — Block double taps and repeat sends. One tap = at most one open
      - Owner: _unassigned_ · Needs: P4-02 · Blocks: P4-06
      - Verify: ten fast taps produce exactly **one** open request in the server log
      - Notes: never auto-retry an open request

- [ ] **P4-05** — Show clear success and clear failure states, and what to do next
      - Owner: _unassigned_ · Needs: P4-02 · Blocks: —
      - Verify: a tester with no training knows if it worked, without asking anyone
      - Notes: "Locker 12 is open" beats a spinner that stops

- [ ] **P4-06** — Handle the network dropping in the middle of an open request
      - Owner: _unassigned_ · Needs: P4-04 · Blocks: —
      - Verify: killing the network mid-request never opens twice and never shows a false success
      - Notes: the app does not know if it opened. Say exactly that, and tell the user to check the door

- [ ] **P4-07** — Show my own open and lock history from the API
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: —
      - Verify: the history in the app matches the server's history rows

- [ ] **P4-08** — Plain message for every refusal: not my locker, wrong mode, locker faulty
      - Owner: _unassigned_ · Needs: P4-02 · Blocks: P6-05
      - Verify: each refusal reason shows its own plain message. No error codes on screen
      - Notes: every reason must be listed in [api-contract.md](../reference/api-contract.md) first

## Safety rules for this phase

- [ ] Confirmed: no open request is fired from a retry loop, a timer, or a screen refresh
- [ ] Confirmed: no open request is fired without a direct user tap
- [ ] Confirmed: when the answer is unclear, the app says it is unclear — it never guesses success

Tick these three with the same care as a task. They are what stops a locker opening by accident.

## Exit check

- [ ] All eight tasks ticked
- [ ] The three safety rules above ticked
- [ ] A person opens and locks a real locker on **both** phones
- [ ] P4-04 and P4-06 proven from the **server log**, not by assumption
- [ ] Counts updated in [README.md](README.md)
