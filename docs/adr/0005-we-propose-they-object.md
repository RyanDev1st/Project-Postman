# 0005 — We propose, they object. And what we cannot confirm, we make adjustable

- **Status:** accepted
- **Date:** 2026-08-05
- **Deciders:** IT team

## Context

Phase 0 was built as a list of questions for other people. Fourteen tasks, nine of which needed an answer from the hardware team or the Server team before anything could move.

Then the teams had their first meeting. **They do not have a plan yet.** There is no Server-team API design to agree with, and no hardware specification to build against. Nobody is holding back answers — the answers do not exist.

A plan that waits for those answers waits forever, and the whole project stalls behind a door nobody is standing at.

## Decision

Two changes to how this team works on this project.

### 1. We write the proposal. Silence is agreement.

Where a question has no owner, **we answer it ourselves**, write the answer down as our proposal, and send it. The other team's job becomes *"object if this is wrong"*, not *"fill in this blank"*.

A blank page asks somebody to do work. A filled page asks them to read. One of those gets answered.

The API contract stops being a form with gaps and becomes **our draft of the whole thing**. Every value in it is a real number we intend to build against, not a `TO AGREE` marker waiting on somebody.

### 2. What we cannot confirm, we make adjustable

Every value we guessed goes in **one settings file**, editable by a person, changeable without touching code and without a new release.

A guess that takes five minutes to correct is not a risk. A guess baked into twelve source files is.

This is what makes decision 1 safe. We are allowed to assume, precisely because being wrong is cheap.

## Why this is not a retreat from contract-first

Rule **C1** says: agree the API contract before building the screen. That rule stands, and this ADR obeys it.

C1 exists because a contract changed *after* the screens are built means rebuilding the screens. That danger is unchanged. What changes is **who writes the contract first** — and C1 never said it had to be the other side.

The contract still exists before the screens. It is still one document. It still needs both teams. We are simply the ones holding the pen, because nobody else has picked it up.

**What stays true from C1:** once the Server team agrees a value, changing it needs both teams again. Being the author does not make it ours to change alone.

## What counts as adjustable, and what does not

Not everything can be a setting. The line:

| Adjustable — goes in settings | Fixed — needs a real decision |
| --- | --- |
| How long a QR code lives | Whether the QR is a key at all ([ADR 0003](0003-parcel-locker-product.md)) |
| How long a token lives | Where the token is stored |
| How long a pickup code lives, how many digits | That the code works once |
| How many wrong tries lock a box, and for how long | That wrong tries lock a box |
| How many days before an uncollected parcel is chased | Who is told |
| Server address, notification route | That the transport is HTTPS |

**The rule: a number is adjustable. A shape is not.**

Changing `300` to `600` is a setting. Changing "the code works once" to "the code works twice" is a design decision and needs an ADR.

## What we accept

| We accept | Because |
| --- | --- |
| **Some of our guesses will be wrong** | They are numbers in one file. Correcting one is a five-minute job, not a rebuild |
| **The Server team may object late** | Later than ideal, but they cannot object to a blank page at all. A proposal is what makes an objection possible |
| **We carry the design load for two teams** | Somebody has to be first. Being first also means the design fits the app, which is the part we are judged on |

## What this does not license

- **Assuming a shape.** Numbers are ours to guess. Whether a door opens without a server is not — see [ADR 0004](0004-offline-pickup.md) and task P0-13.
- **Assuming quietly.** Every guess is written down with its default and a note that it is a guess. An assumption nobody can find is the expensive kind — rule A3.
- **Assuming about safety.** Anything touching a door opening, a token or a code still gets the careful review of rule F6, guess or not.

## Affects

- `docs/reference/api-contract.md` — becomes our full proposal, with a settings table instead of scattered `TO AGREE` markers
- `docs/reference/architecture.md` — gains the settings file as a named part of the system
- `docs/roadmap/phase-0-agree.md` — tasks change from *"agree X with them"* to *"decide X, write it down, send it"*
- A new task for the settings file itself, so the tunables have somewhere to live before Phase 1 starts
