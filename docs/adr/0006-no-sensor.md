# 0006 — No sensor. The door closing is the evidence

- **Status:** accepted
- **Date:** 2026-08-05
- **Deciders:** IT team

## Context

A teammate said in chat that each box would have a sensor — *"Sẽ có sensor"*. The design was built on it: a delivery was recorded when the sensor saw the parcel, not when the door shut.

That was worth building on, because it killed a real problem. A driver could open a door, walk away still holding the parcel, and the system would record a delivery that never happened. The sensor made that impossible.

**There is no sensor.** Confirmed by the team on 2026-08-05.

## Decision

**The door closing is the evidence.** A delivery is recorded when the driver confirms and the door shuts. A collection is recorded when the receiver's door shuts.

The problem the sensor solved comes back. We accept it — see below.

## Why we accept it rather than replace it

Three replacements were possible, and all three are worse:

| Instead of a sensor | Why not |
| --- | --- |
| The driver taps "I put it in" | Same trust, one more tap. A driver willing to lie will tap the button |
| A camera in each box | Cost, wiring, privacy, and a photograph of the inside of a box that somebody has to store |
| Weigh the box | That is a sensor |

The honest position is that without hardware, **the system cannot know whether a parcel is physically in a box.** Adding steps that pretend otherwise buys nothing and costs a worse experience for every honest driver.

## What we accept

| We accept | Why it is tolerable here |
| --- | --- |
| **A driver can record a delivery without leaving the parcel** | The receiver opens an empty box and reports it. The history says which driver, which box, what time — so it is traceable after the fact, just not preventable before it |
| **A receiver can open a box and leave the parcel inside** | The box shows as empty when it is not. The next drop into that box finds it occupied. Rare, and visible |

Both are recoverable, and both leave a trail. Neither loses a parcel silently.

This fits the threat model in [ADR 0004](0004-offline-pickup.md): a guard post, low-value contents, delay rather than prevention. A courier who steals parcels gets caught by the second complaint, not by a sensor.

## The design does not depend on this

Deliberately. The contract records **"the drop is complete"** as an event, and does not say what produced it.

Today a door closing produces it. If a sensor is fitted later, the sensor produces it instead. **The server, the app, and the cabinet screen do not change** — only the thing that fires the event.

This is the same principle as [ADR 0005](0005-we-propose-they-object.md): what we cannot confirm, we make cheap to change.

## When to revisit

- A parcel goes missing and nobody can say whether it was ever put in.
- Real drivers turn out to be careless rather than dishonest — doors left open, wrong boxes.
- The hardware team offers a sensor for little money. It is a straight improvement, and it costs one endpoint change.

## Affects

- `docs/reference/architecture.md` — the sensor is out of the diagram and out of the flow
- `docs/reference/api-contract.md` — endpoint 12 becomes "the door closed", with the event named so a sensor can replace it later
- `docs/reference/how-it-works.html` — the sensor step is gone
- `docs/roadmap/` — P0-10 dropped, P3-05 and P5-04 rewritten, P6-04 gains the accepted risk
- Assumption **A-10** — dropped. There is no sensor to ask about
