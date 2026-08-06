# 0009 — Start Phase 1 before Phase 0 is finished

- **Status:** accepted
- **Date:** 2026-08-06
- **Deciders:** Ryan, for the IT team

## Context

Phase 0 was built on one rule: settle everything that costs the most to change late, and write no code until it is settled. That rule assumed the answers would arrive.

Nine of the seventeen tasks are still open, and **every one of them waits on somebody outside this team** — the hardware team for the screen and the cabinet, the Server team for the contract and the key, the whole team for a list of phone models. None has moved in a day. Meanwhile the app that all of it exists to serve has not been started.

Phase 0 has stopped preventing rework and started preventing work.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| A — hold the gate | Nothing is built twice | Nothing is built at all. We wait on people who are waiting on us |
| B — drop the gate entirely | Fastest start | The four genuinely expensive questions get answered by whoever writes the code first |
| C — keep the gate on the API shape only, defer the rest | Work starts now, and the one thing that is expensive to change late stays protected | Some Phase 0 answers arrive after code exists, and that code changes |

## Decision

We choose **C**.

**One thing still gates Phase 1: the shape of the API.** Not the paths, not the numbers, not the error codes — the shape. Which calls exist, what each one is for, and which caller may make it. That is the P/s note on the board: *agree with the Server team on the shape of the API*.

Everything else in Phase 0 is deferred and marked `⏸️ LATER`, with the reason on the task.

| Task | Deferred because |
| --- | --- |
| **P0-02** cabinet screen spec | Unknown, and nobody can say when. Build the screen to scale to any size instead |
| **P0-04** send the contract | The shape is what matters. Marginal changes can be made later |
| **P0-05** cabinet key | Keep it simple now, redefine it before the cabinet is real |
| **P0-08** masked name format | Ours to propose; redefine when the Server team answers |
| **P0-16** a real cabinet | No cabinet is coming soon enough to wait for. Build against the simulator |
| **P0-17** oldest team phone | A low `minSdk` covers everyone without asking. Revisit if a phone actually fails |

**P0-14 is decided, not deferred: the cabinet screen is a web page.** HTML, CSS and JavaScript, laid out to fit any screen it lands on.

Deferring P0-14 as well would have taken Phases 3, 5 and most of 6 with it — every cabinet task sits behind it — and that is the whole shipper and pickup flow, which is the product. Choosing web is what keeps them reachable.

Web is the only stack that survives P0-02 being unknown. A native app must be written for a named operating system, and nobody can name it. A web page runs on whatever the screen turns out to be: an Android tablet, a mini PC, a Raspberry Pi with a browser. Until the hardware exists it runs full-screen in a browser on a laptop, which is enough to build and demonstrate the whole shipper flow.

This is a different stack from the phone app, and that is deliberate — [ADR 0001](0001-tech-stack.md) allows it. The two front-ends share no code either way; they share the API contract.

**The layout has one rule while P0-02 is open:** never assume a size. No fixed pixel widths, and it must be readable at both a small tablet and a large monitor. A screen size arriving late then costs a test, not a rewrite.

**`minSdk` is 24 — Android 7.0.** Chosen without asking anybody, which is the point of deferring P0-17.

24 is low enough that no phone anyone on this team is likely to own falls below it, and high enough to stay on documented modern Android. Jetpack Compose runs from 21, so this costs nothing in tooling. If a team member turns out to have something older, lowering it later is a settings change and a re-test, not a rewrite — the risk P0-17 was written to avoid was raising it later, which drops users. Lowering is safe.

## Where these decisions are written

**Not in [ADR 0001](0001-tech-stack.md).** An ADR is never edited — that is the rule in `CLAUDE.md` — so the cabinet stack and the `minSdk` value live here, in the ADR that decided them. P0-14 and P0-17 were written expecting ADR 0001 to grow, and their Verify lines now point here instead. That redirects the check; it does not weaken it.

**P0-12** is decided rather than deferred: the offline design is the challenge-and-response one, using cryptography. That accepts [ADR 0004](0004-offline-pickup.md), which means **P0-13 is now urgent, not optional** — ADR 0004 breaks rule C4, and nothing offline may be built until that conflict is settled.

## Why

The four questions Phase 0 called expensive are only expensive if they change **after** the screens are built. Three of them — the screen size, the cabinet key, the masked format — change one file each, because every call goes through one network file (P1-04) and every guessed number lives in one settings file (P0-15). We already paid for the ability to change our minds. Refusing to start is paying twice.

The API shape is the exception, and it is the only one. Change the shape and the screens are rebuilt.

## What we accept

**A deferred task is not a finished task.** Each one comes back, and the board still counts it. Progress will look worse than a board that quietly dropped them, which is the point.

**The simulator does not become evidence.** [ADR 0008](0008-cabinet-simulator.md) still stands, unchanged. Building against the simulator is allowed; *proving* against it is not. Every Verify line in Phases 3, 5 and 6 that names real metal still names real metal, and those tasks stay unticked until a cabinet exists. This decision unblocks writing the code, not ticking the box. If that ever feels like a technicality, re-read ADR 0008 — it predicted exactly this feeling.

**`minSdk` is now a guess, not a measurement.** We set it low enough that it is very unlikely to exclude anybody, and we find out we were wrong the first time a team member cannot install the app. Logged as an assumption with a way to check it.

**P0-13 was already the sharpest task on the board and is now blocking.** Deciding P0-12 by note does not decide the rule conflict it creates.

## Affects

- Unblocks **P1-01**, and through it the whole of Phase 1.
- Marks `⏸️ LATER`: P0-02, P0-04, P0-05, P0-08, P0-16, P0-17.
- Closes **P0-14**: the cabinet screen is a web page. Keeps Phases 3, 5 and 6 reachable.
- Decides **P0-12** in favour of challenge-and-response; raises **P0-13** to the next thing that must be settled.
- Reverses "Do every Phase 0 task before any Phase 1 task" in `docs/roadmap/README.md` and `docs/roadmap/phase-0-agree.md`.
- Sets `minSdk` to **24** as a guess, and adds an assumption to `working-rules.md`.
- Redirects the Verify lines of **P0-14** and **P0-17** from ADR 0001 to this ADR, because ADR 0001 may not be edited.
