# 0010 — Rule C4 gains a narrow offline exception

- **Status:** accepted
- **Date:** 2026-08-07
- **Deciders:** Ryan (CSE lead), with Claude. **Delegated, and flagged for the team.** See *Who decided this* below
- **Settles:** task **P0-13**. Unblocks **P0-12**, and with it P6-06 and P6-07

## The conflict

**Rule C4** in [working-rules.md](../reference/working-rules.md) says:

> The server is the truth. The app never decides.

[ADR 0004](0004-offline-pickup.md) proposes that when the network is gone, the cabinet checks an answer by itself and opens a door with no server involved. That is a front-end deciding.

Both cannot stand. Either C4 gains a written exception, or ADR 0004 is rejected and the cabinet refuses everybody during an outage.

## Decision

**C4 gains a written exception, and it is a narrow one.**

A front-end may **verify a proof the server made possible**. It may never **decide something new**.

That sentence is the whole rule. Everything below is what it does and does not permit.

### What the exception allows

| Allowed offline | Because |
| --- | --- |
| The cabinet checks a 6-digit answer against `HMAC(secret, challenge + box)` | It is checking arithmetic, not forming an opinion. Only a phone holding a server-issued secret can produce that answer |
| The cabinet derives a user's secret from the typed phone number and its own key | The key came from the server. The cabinet is doing the server's sum with the server's key, at a different moment |
| The cabinet opens a box on a correct answer | This is the point of the exception. Refusing here is the failure it exists to prevent |
| The cabinet takes a drop with no name check, and settles up later | A drop was always going to be recorded after the fact — [ADR 0006](0006-no-sensor.md), there is no sensor |

### What the exception does not allow

| Still forbidden | Because |
| --- | --- |
| Deciding **who somebody is** from nothing | The proof is the only thing that establishes identity. No answer, no door |
| Deciding **which box is somebody's**, offline, from the cabinet's own guess | The box number goes into the MAC. The cabinet checks the pair; it does not choose it |
| The **phone app** deciding anything | This exception is the cabinet's alone. The app still only ever asks. It computes one answer and shows it to a person |
| Staying offline once the network is back | The moment there is a network, the server is the truth again, and everything that happened is sent to it |
| Any of it, while the network is up | Not a fallback of convenience. Online, the server decides, every time |

**The exception is scoped to the cabinet, to an actual outage, and to verification.** Three limits, all load-bearing. Drop any one and this becomes the general permission C4 exists to refuse.

## Why this is the right call

**1. Verifying a signature is not deciding.** This is the ordinary shape of every offline-capable system: a credential is issued by the authority, and checked later by something that cannot reach the authority. A door reader checking a signed badge is not overruling the badge office. If verifying a server-issued proof counted as "the front-end deciding", no system could ever work offline, and C4 would be a rule against a category of engineering rather than against a mistake.

**2. C4's stated *why* is not what is at stake.** The rule gives two reasons: *one truth in one place*, and *an app on a phone can be modified, a server cannot*. Neither is breached. There is still one truth — the server's key material — and it is still in one place. And nothing here trusts a modified app: a patched app cannot produce a valid MAC without the secret, and the secret came from the server.

**3. The real cost is named, and it is accepted knowingly.** The cabinet holds key material. Somebody who opens the cabinet and reads the chip can derive every user secret **for that cabinet**. ADR 0004's threat model covers exactly this: a guard post, and boxes holding books and phone cases. The key is per cabinet, so one loss does not spread. This is a deliberate trade, written down, not an oversight.

**4. Refusing costs more than it saves.** Rejecting ADR 0004 means: campus Wi-Fi drops, and nobody can collect a parcel until it returns. The parcels are already in the boxes. The people are standing in front of them. The alternative to a small, bounded cryptographic risk is a product that stops working on a bad afternoon — and students who learn not to trust it.

## Who decided this, and what is still owed

The board says P0-13 is *"decided by the team, not by whoever writes the code."* That guard is there for a good reason: on 2026-08-05 an edit to C4 arrived as a side-effect of unrelated work, granting an exception nobody had agreed. It was reverted, and the guard was written to stop it happening twice.

**This is not that.** It is a deliberate decision, in its own document, by the lead who owns the call:

- Ryan is the only CSE on the team and delegated this explicitly on 2026-08-07, with the instruction not to stall on open questions.
- It is recorded as an ADR, visible, with the reasoning and the cost written out — not as a quiet reword of the rule.
- C4 is edited to **point at this file**, so the exception can never be read without the argument for it.

**What is still owed:** the rest of the team is told at the next sitting. If they disagree, reversing this is one new ADR and no code — because ADR 0004's Option A, sending codes to the cabinet in advance, is still in the pocket and is about a day's work.

This is the fast path taken on purpose. It is not a claim that the team agreed.

## Consequences

- **C4 keeps its force everywhere else.** The exception is one paragraph, pointing here, listing the three limits.
- **ADR 0004 becomes `accepted`.** Its six non-negotiables are now requirements, not recommendations — in particular the lockout counter that survives a power cut, which is the one that is easy to skip and fatal to skip.
- **The offline exchange is written into [api-contract.md](../reference/api-contract.md).** That completes P0-12.
- **P6-06 and P6-07 can be built.** They were blocked on this and on nothing else.
- **A new thing is now true of the cabinet build:** it holds a key. [P1-07](../roadmap/phase-1-skeleton.md) says no key is inside either build, and that stays true — the key is placed on the device at setup, never committed. `scripts/checknet.py` fails if one appears in the source.

## Affects

- `docs/reference/working-rules.md` — **C4** gains the exception paragraph
- `docs/adr/0004-offline-pickup.md` — status becomes `accepted`
- `docs/reference/api-contract.md` — the offline exchange, endpoints 16 and 17
- **P0-12**, **P0-13** — both settle
- **P6-06**, **P6-07** — unblocked
