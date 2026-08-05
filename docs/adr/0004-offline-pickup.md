# 0004 — Picking up a parcel when the cabinet is offline

- **Status:** proposed. **Not decided.** Task **P0-12** settles it
- **Date:** 2026-08-05
- **Deciders:** IT team, with the hardware team

## Context

The cabinet is on Wi-Fi. Campus Wi-Fi drops.

The team was asked *"nếu mất mạng thì bên mình có cách giải quyết chưa"* — do we have a way to handle a lost network yet? The answer was **"Dạ chưa"**. No. This ADR exists to close that hole.

## The one fact that makes this small

**A drop can never work offline. Only a pickup can.**

An offline cabinet cannot look up whose phone number `0909618328` is. It has no user list and must never have one — a public terminal that holds every student's details is a data leak on wheels. So when the network is down, a driver simply cannot deliver, and the screen says come back later.

That leaves one job: **let somebody collect a parcel that is already inside.** The cabinet already knows the parcel is in box 04, because it was put there while the network was up.

This turns a hard problem into a much smaller one.

## The idea on the table

Proposed by the IT team, modelled on Google Authenticator:

1. The cabinet loses the network and shows a QR.
2. The QR holds a value worked out from the parcel's own details — the phone number, the box.
3. The phone scans it, runs an algorithm, and produces **6 digits**.
4. The user types those 6 digits into the cabinet. Correct → the door opens.

The **shape** of this is right. Cabinet asks a question, phone answers it, the answer is short enough to type. That is a challenge–response protocol, and it is exactly the correct pattern for this problem. Keep the shape.

Three things inside it have to change before it is safe.

### 1. "The algorithm only we know" does not hold

The plan was *"thuật toán có mình biết thôi"*.

The algorithm ships inside the app, on students' phones. An Android app can be pulled apart in an afternoon with free tools. Once one person reads it, the algorithm is public — and if the algorithm was the only secret, every box on campus opens.

This has a name. **Kerckhoffs's principle:** a system must stay safe even when everything about it except the key is public. Every cipher in real use is published on purpose, precisely so that thousands of people can attack it and prove it holds.

**Fix:** publish nothing, hide nothing — use a **standard, public algorithm** (HMAC-SHA256, which is what Google Authenticator uses), and keep a **secret key**. The key is the secret. Never the method.

### 2. Encryption is the wrong tool

The plan was to *encrypt* the phone number.

Encryption hides a value from people who should not read it. That is not the problem here — the phone number is not a secret, and the person standing at the cabinet already knows it.

The real question is *"can you prove you hold the secret?"* That is **authentication**, and the tool is a **MAC** (a message authentication code), not encryption.

It matters practically: if the code is worked out from the phone number alone, then anybody who knows a phone number and the algorithm can produce a valid code. Point 1 says the algorithm will be known. So the code must depend on a **secret that only the right phone and the cabinet hold**.

### 3. The cabinet has to check the answer with no network

This is the part the plan does not cover yet, and it is the hardest part.

Offline, the cabinet cannot ask the server "is 418 293 correct?". It has to work that out itself — which means the cabinet must hold key material.

That is a real risk. Steal one cabinet, open it, read the key, and you may be able to open every box on every cabinet forever. Whatever is chosen, the key must be **per cabinet**, never one master key shared by all.

## Two options

### Option A — the cabinet is told the codes in advance

While the network is up, the server sends each cabinet the codes for the parcels currently inside it. The cabinet keeps that short list. The network drops; the cabinet already knows which codes are good.

| Good | Bad |
| --- | --- |
| No cryptography to design, get wrong, or defend | The cabinet holds live codes. They must be wiped the moment a parcel leaves |
| No key on the cabinet beyond the short list | Only covers parcels dropped **before** the network went down |
| The phone does not need to be working at all | A code issued during the outage cannot reach the cabinet |
| Same typed code as the normal backup path (P5-08) — one thing to build, not two | |

Covers the ordinary outage: a parcel goes in while things work, the network drops an hour later, the student still collects.

### Option B — challenge and response, the original idea done properly

The cabinet shows a challenge. The phone computes an answer with a **per-user secret** and a **published algorithm**. The cabinet checks it with the same secret.

| Good | Bad |
| --- | --- |
| Works even for a parcel the cabinet learned about late | Needs a per-user secret stored on the cabinet |
| No live codes sitting on the cabinet | A stolen cabinet is a serious event — the secrets need real hardware protection |
| The genuinely stronger design, if built right | The clocks must agree. An offline cabinet's clock drifts, and can be attacked |
| | A lockout counter must survive a power cut, or an attacker unplugs and keeps guessing |
| | Real cryptographic work. Easy to get subtly wrong |

## Recommendation

**Start with Option A.** Move to Option B only if a real outage shows that A is not enough.

Reasons, in order:

1. **A covers the common case and B is only needed for the rare one.** The parcel is almost always dropped while the network is up. That is the whole of A's scope.
2. **A has no cryptography to get wrong.** Home-made crypto in a student project is where marks and real security both go to die. Nothing in A can be subtly broken.
3. **A and the normal backup are the same thing.** P5-08 already builds a typed code. Option A is that code, cached on the cabinet. Option B is a second, separate mechanism.
4. **B is not ruled out.** It is written down here, properly, so it can be built later without starting the thinking again.

**What is not negotiable, whichever is chosen:**

- No secret algorithm. A published one, with a real key.
- One key per cabinet, never one key for all of them.
- Every offline code works once, and expires.
- Wrong tries lock the box, and that count survives a power cut.
- Everything that happened offline is sent to the server the moment the network returns. An outage must not lose the record of who opened what.

## Open questions

1. How often does campus Wi-Fi actually drop, and for how long? Nobody has measured it. If it is twice a term for ten minutes, Option A is plainly enough. **Measure before building either.**
2. Can the cabinet keep time without a network? Option B needs it. A cheap board without a battery-backed clock cannot.
3. What does the cabinet do about a **drop** during an outage? Current answer: refuse, and say come back. Confirm that is acceptable.

## Affects

- `docs/roadmap/phase-0-agree.md` — **P0-12** decides this
- `docs/roadmap/phase-6-faults.md` — **P6-06** builds and checks it
- `docs/reference/api-contract.md` — the chosen way gets written up there
- Assumption **A-09** — this ADR only matters because the cabinet is online in the normal case
