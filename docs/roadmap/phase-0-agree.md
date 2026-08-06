# Phase 0 — Agree

**Goal:** settle everything that costs the most to change late. No code in this phase.

**Progress: 11 / 17.**  ·  6 deferred `⏸️ LATER`

Phase 0 is the cheapest phase and it prevents the most rework. **It no longer gates Phase 1** — see [ADR 0009](../adr/0009-start-phase-1-early.md).

## Where this phase stands

Nine ticked, six deferred, two left.

| Left to do | Why it is still here |
| --- | --- |
| **P0-13** — does rule C4 gain an offline exception? | The sharpest question on the board, and now the most urgent. Nothing offline gets built until it is settled |
| **P0-12** — the offline design | The way is chosen (challenge and response). It needs writing into `api-contract.md`, and ADR 0004 marked `accepted` — which is what raises P0-13 |

Both are ours. Neither waits on another team.

```
   P0-03 ✅ ──> P0-15 ✅ ──┐
   P0-09 ✅ ──────────────┼──> (phone app, Phase 1 — open)
   P0-17 ⏸️ minSdk 24 ────┘

   P0-14 ✅ web page ─────────> (cabinet screen, Phase 1 — open)
   P0-02 ⏸️ size unknown, so the layout scales

   P0-11 ✅ ──> P0-12 ──> P0-13   <- the only live chain left
```

**Everything deferred was waiting on somebody else.** P0-02 and P0-16 on the hardware team, P0-04, P0-05 and P0-08 on the Server team, P0-17 on a group-chat answer. None of them moved in a day, and the app they exist to serve had not been started.

## Who answers the deferred ones

They come back. This is who closes them when they do.

| Who | Tasks | What is needed |
| --- | --- | --- |
| **Hardware team** | P0-02 ⏸️, P0-16 ⏸️ | A screen spec, and a cabinet we can touch |
| **Server team** | P0-04 ⏸️, P0-05 ⏸️, P0-08 ⏸️ | Agree the **shape** of the API first — that one is not deferred. Then the key, then the masked format |
| **Us, the IT team** | P0-12, P0-13 | Ours alone, and the only thing left running |
| **The group chat** | P0-17 ⏸️ | One question, whenever somebody cannot install the app |

## Tasks

**Six tasks here are now `⏸️ LATER`, by [ADR 0009](../adr/0009-start-phase-1-early.md).** Phase 0 stopped preventing rework and started preventing work — every open task waited on somebody outside this team, and none moved. Phase 1 no longer waits for them.

**One thing still gates Phase 1: the shape of the API.** Which calls exist, what each is for, and which caller may make it. Not the paths, not the numbers, not the error codes — those change one file each. Change the shape after the screens exist and the screens are rebuilt.

- [x] **P0-01** — Ask the hardware team: does the cabinet have its own network connection?
      - Owner: Team · Needs: — · Blocks: P0-02, P0-04, P0-12, P0-16, P1-03
      - Verify: a written answer from the hardware owner naming SIM or Wi-Fi, in `architecture.md`
      - Done: 2026-08-05 — The team confirmed the cabinet connects to Wi-Fi. That is now written into `architecture.md` as a fact rather than a guess, and the diagram says so. Everything in the plan needed this answer, so it unblocks most of the phase. Worth knowing the answer came from our own team rather than the hardware owner directly — if that turns out to be wrong, this is the first thing to re-check.

- [ ] **⏸️ LATER — P0-02** — Write down what the cabinet screen is: size, touch, what it runs
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: —
      - Verify: the screen size, whether it takes touch, and its operating system are written in `architecture.md`
      - Notes: deferred by [ADR 0009](../adr/0009-start-phase-1-early.md) — unknown, and nobody can say when. The screen is built to **scale to any size** instead: no fixed pixel widths, readable on a small tablet and a large monitor. A size arriving late then costs a test, not a rewrite

- [x] **P0-03** — Pick the tech stack for the **phone app**
      - Owner: Team · Needs: — · Blocks: P0-15, P1-01
      - Verify: [ADR 0001](../adr/0001-tech-stack.md) is `accepted` for the phone app, and `CLAUDE.md` names the real stack
      - Done: 2026-08-05 — Chose to build the Android app natively, in Kotlin. Nobody on the team has used any of the options before, so the usual tie-breaker of "use what you already know" did not apply. Native won because when a beginner hits a problem the error message is Android's own and the first search result answers it — with a cross-platform tool the same problem arrives translated, and you cannot tell whose fault it is. The cost is real and we took it knowingly: an iPhone version later means writing the app a second time, not converting it. The design work carries over even though the code will not.

- [ ] **⏸️ LATER — P0-04** — Write the whole API contract as our proposal, and send it
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: —
      - Verify: `api-contract.md` has zero `TO AGREE` markers **and** it has been sent to the Server team with a date recorded here
      - Notes: deferred by [ADR 0009](../adr/0009-start-phase-1-early.md) — marginal changes can be made later. The document is written and we build against it, per [ADR 0005](../adr/0005-we-propose-they-object.md): silence means agreement. **What is not deferred is the shape** — which calls exist and who may make them. Agree that with the Server team before Phase 2, or the screens get rebuilt

- [ ] **⏸️ LATER — P0-05** — Agree how the cabinet proves it is the cabinet
      - Owner: _unassigned_ · Needs: — · Blocks: —
      - Verify: the key type, how it is placed on the cabinet, and how it is replaced are written in `api-contract.md`
      - Notes: deferred by [ADR 0009](../adr/0009-start-phase-1-early.md) — keep it simple now, redefine before the cabinet is real. Until then the cabinet sends a plain key we issue ourselves, and it is a **test key, never a real one**. Get this wrong and a stolen cabinet opens every box, so this must be settled before any cabinet leaves a desk. The key never enters this repo

- [x] **P0-06** — Decide how the receiver is told a parcel arrived
      - Owner: Team · Needs: — · Blocks: P0-11, P4-01
      - Verify: one route is chosen and written down, with its cost per message, and it is adjustable without a new release
      - Done: 2026-08-05 — Chose push through the app. It costs nothing per message, and the app is already needed to collect a parcel, so nobody has to install anything extra. SMS would cost money for every single delivery. The choice sits in the settings list, so if push turns out to be unreliable on campus we can switch to Zalo or SMS by editing one line, not by rebuilding the app.

- [x] **P0-07** — Decide the QR session code: what it holds, how long it lives
      - Owner: Team · Needs: — · Blocks: P5-01
      - Verify: the lifetime in seconds and the refresh rate are written down, and both are adjustable without a new release
      - Done: 2026-08-05 — Set the code on the cabinet screen to last 60 seconds, with the screen drawing a new one every 30 seconds. Both numbers are guesses, so both went into the settings list where anyone can change them. If people find it expires while they are still walking up, the number goes up; nothing else has to change. The code itself only says which cabinet and when, so a longer life is not dangerous the way the typed code would be.

- [ ] **⏸️ LATER — P0-08** — Agree what the shipper sees when he looks up a receiver
      - Owner: _unassigned_ · Needs: — · Blocks: —
      - Verify: the exact masked format is written in `api-contract.md` with a worked example, **and the Server team has confirmed they can produce it**
      - Notes: deferred by [ADR 0009](../adr/0009-start-phase-1-early.md) — redefine when the Server team answers. We build `Nguyễn V. A***` meanwhile. **What is deferred is the shape of the mask, not the masking** — never a full name, never a phone number, because the cabinet screen is a public terminal

- [x] **P0-09** — List the test devices, and stand up the cabinet simulator
      - Owner: Team · Needs: — · Blocks: P1-01
      - Verify: the table below names a real Android phone and its OS version, **and** `open_door(4)` swings the right door in the Blender simulator
      - Notes: Android only — [ADR 0007](../adr/0007-android-first.md). Simulator is [ADR 0008](../adr/0008-cabinet-simulator.md). **Not** a substitute for a real cabinet — that is P0-16
      - Done: 2026-08-05 — Named the test phone: a Vivo X200 Pro running OriginOS 6. The OS version is as reported, not yet read off the phone, so it is marked to confirm. Then ran the cabinet simulator in Blender: it built 19 doors with no door 06, exactly as the drawing shows. Asked it to open box 4 and door 04 turned, on its own, by 105 degrees — the other 18 did not move at all. So the thing that will later tell a real cabinet to open a box already talks to the model correctly. This does not prove anything about real metal; that is P0-16.

- [ ] **⏸️ LATER — P0-17** — Find the oldest Android phone on the team, and set `minSdk`
      - Owner: _unassigned_ · Needs: — · Blocks: —
      - Verify: every team member's Android version is listed, and the recorded `minSdk` in [ADR 0009](../adr/0009-start-phase-1-early.md) is confirmed to cover the oldest one
      - Notes: deferred by [ADR 0009](../adr/0009-start-phase-1-early.md), which guesses **`minSdk` 24 — Android 7.0** instead of asking. Low enough that nobody on the team is likely to fall below it. Lowering it later is safe; raising it later drops users, which is the risk this task existed to avoid. Closes the first time someone tries to install and cannot

- [x] **P0-10** — Decide what records a delivery, now that there is no sensor
      - Owner: Team · Needs: — · Blocks: P3-05, P5-04
      - Verify: [ADR 0006](../adr/0006-no-sensor.md) is `accepted`, and `api-contract.md` endpoint 12 names the event rather than what causes it
      - Done: 2026-08-05 — The team confirmed there is no sensor in the boxes, which reverses an earlier plan. So the door closing is what records a delivery. That means a driver could open a door, walk off still holding the parcel, and the system would think it was delivered. We looked at three ways to stop that — a confirm button, a camera, a weight check — and all three were either the same trust problem or a sensor by another name. We accept the risk: it leaves a trail naming the driver, the box and the time, so it is traceable afterwards even though it is not preventable beforehand. The call is named "a door closed" so that if a sensor is ever fitted, it fires the same call and nothing else changes.

- [x] **P0-11** — Decide the typed pickup code: length, lifetime, and lockout
      - Owner: Team · Needs: P0-06 · Blocks: P0-12, P5-08
      - Verify: the length, the lifetime, and the number of wrong tries before a box locks are all written in `api-contract.md`, and all three are adjustable
      - Done: 2026-08-05 — Settled on a 6-digit code that lasts 48 hours, with the box locking for 15 minutes after 5 wrong tries. 48 hours because a parcel usually sits a day or two and nobody should be punished for being busy. The lockout matters more than it looks: 6 digits is one chance in a million per try, which is only safe while somebody cannot sit there guessing all night. All four numbers are in the settings list. What is **not** adjustable is that the code works once and never repeats — that is a design decision, not a number.

- [x] **P0-12** — Decide how a pickup works when the cabinet loses the network
      - Owner: Team · Needs: P0-01, P0-11 · Blocks: P0-13, P6-06, P6-07
      - Verify: [ADR 0004](../adr/0004-offline-pickup.md) is `accepted`, and the chosen way is written in `api-contract.md`
      - Notes: **the way is chosen — challenge and response, using cryptography**, per [ADR 0009](../adr/0009-start-phase-1-early.md). Depended on P0-13, which [ADR 0010](../adr/0010-c4-offline-exception.md) settled
      - Done: 2026-08-07 — Wrote down exactly how somebody collects a parcel when the cabinet has lost its internet, which until now was a plan nobody had written out. She types her number, the cabinet shows a random pattern, her phone turns that into six digits, she types them in, and the cabinet checks them on its own. It works because her phone was given a private number when she signed up, and the cabinet can work out the same one — so neither of them needs the internet at that moment. Two new messages were added to the list we are sending the Server team: one to hand a phone that private number when it registers, and one for the cabinet to report everything that happened while it was offline, so nothing is ever lost. The method itself is public on purpose; only the key is secret, because the app is on students' phones and anyone can take it apart.

- [x] **P0-13** — Settle whether rule C4 gains an offline exception
      - Owner: Ryan · Needs: P0-12 · Blocks: —
      - Verify: rule **C4** in `working-rules.md` either carries a written exception, or ADR 0004 is marked rejected. One or the other, decided by the team, not by whoever writes the code
      - Notes: settled by [ADR 0010](../adr/0010-c4-offline-exception.md) — C4 gains a **narrow** exception: a front-end may verify a proof the server made possible, never decide something new. Scoped to the cabinet, to a real outage, and to verification only. **Decided under delegation, not at a team sitting — see the ADR, and tell the team**
      - Done: 2026-08-07 — Settled the sharpest disagreement on the board. One of our own rules says the app and the cabinet never decide anything themselves — they always ask the server. But the plan for working without internet has the cabinet deciding to open a door on its own. Both could not be true. The answer we chose: the cabinet is allowed to *check* an answer, because only a phone holding a number the server gave it can produce that answer — but it is never allowed to decide who somebody is, or which box is theirs, on its own. Three limits are written into the rule so this cannot quietly grow: the cabinet only, during a real outage only, checking only. Written up properly in its own decision file rather than by quietly editing the rule, because that exact quiet edit happened once before and had to be undone. Ryan made the call; the rest of the team still needs telling.

- [x] **P0-14** — Pick the tech stack for the **cabinet screen**
      - Owner: Team · Needs: — · Blocks: P1-03
      - Verify: [ADR 0009](../adr/0009-start-phase-1-early.md) names the cabinet stack, and says whether it is the same as the phone app or different
      - Notes: the Verify moved from ADR 0001 to ADR 0009 — an ADR is never edited, so the decision lives in the ADR that made it
      - Done: 2026-08-06 — Chose to build the cabinet screen as a web page. Nobody can tell us yet what the screen is or what it runs, and a web page is the only choice that does not need that answer — it works on a tablet, a small computer, or anything with a browser. Until a cabinet exists it runs full-screen on a laptop, which is enough to build and show the whole shipper flow. This is a different tool from the phone app, on purpose. We nearly deferred this one too, and that would have stopped work on the drop and the pickup — which is the product.

- [x] **P0-15** — Build the settings file, before any code is written  
      - Owner: Team · Needs: P0-03 · Blocks: P1-01, P1-08
      - Verify: every number in the settings table in `architecture.md` is in `config/settings.json`, and a person can edit it without touching code
      - Notes: makes guessing safe — [ADR 0005](../adr/0005-we-propose-they-object.md). The "without a new release" half moved to **P1-08**: it needs a running app, and P0-15 blocks P1-01 which builds it
      - Done: 2026-08-05 — Made `config/settings.json`, holding all ten numbers we guessed. Anyone can open it and change one. The server address is left blank on purpose — the real one must never sit in this repo. Getting a changed number onto a phone without rebuilding is not done yet; that is P1-08.

- [ ] **⏸️ LATER — P0-16** — Get a real cabinet to test on
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: —
      - Verify: a real cabinet, or at least one real door with a real lock and the real screen, is somewhere the team can physically reach it
      - Notes: deferred by [ADR 0009](../adr/0009-start-phase-1-early.md) — no cabinet is coming soon enough to wait for, so we **build** against the simulator. **The simulator still does not close this task and cannot.** [ADR 0008](../adr/0008-cabinet-simulator.md) predicted that a good simulator would make hardware feel less urgent; that is now happening, which is the reason to keep this open rather than to close it. Every Phase 3, 5 and 6 Verify line that names real metal still names real metal

## Test devices

Fill this in during P0-09.

| Device | Model | OS version | Who holds it |
| --- | --- | --- | --- |
| Android phone — main | Vivo X200 Pro | OriginOS 6, Android 16 — *reported, confirm in Settings › About* | the user |
| Android phone — oldest on the team | *(P0-17 — not yet collected)* | — | — |
| iPhone | *(later — [ADR 0007](../adr/0007-android-first.md))* | — | — |
| Real cabinet | *(P0-16 — not yet)* | — | — |
| Cabinet simulator | Blender, `scripts/cabinet-sim/` | — | anyone |

The X200 Pro is a 2024 flagship on the newest OS. It will find the fewest problems of any phone on the team. `minSdk` is **24 — Android 7.0**, guessed rather than measured ([ADR 0009](../adr/0009-start-phase-1-early.md)). The oldest team phone confirms or lowers it — that is P0-17, deferred.

## Exit check

- [ ] All seventeen tasks ticked — **6 are `⏸️ LATER`, so this phase cannot close yet, on purpose**
- [x] `api-contract.md` has zero `TO AGREE` markers — done 2026-08-05
- [x] [ADR 0001](../adr/0001-tech-stack.md) is `accepted` — Kotlin, native Android, done 2026-08-05
- [x] The cabinet is confirmed to have a network connection — Wi-Fi, done 2026-08-05
- [ ] Counts updated in [README.md](README.md)
