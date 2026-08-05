# 0004 — Working when the cabinet loses the network

- **Status:** proposed. **Not decided.** Task **P0-12** settles it
- **Date:** 2026-08-05
- **Deciders:** IT team, with the hardware team

## Context

The cabinet is on Wi-Fi. Campus Wi-Fi drops.

The team was asked *"nếu mất mạng thì bên mình có cách giải quyết chưa"* — is a lost network handled yet? The answer was **"Dạ chưa."** No. This ADR closes that hole.

## What we are protecting against, and what we are not

Written down first, because every choice below follows from it.

| | |
| --- | --- |
| **The cabinet stands next to a guard post.** | People are watching it. Somebody carrying a cabinet away, or attacking it with tools, is not our problem to solve in software |
| **This is a university parcel locker.** | The contents are books, clothes, and phone cases. Not cash, not exam papers, not medicine |
| **The goal is to delay, not to be impenetrable.** | Anyone determined enough gets in. We make it slow and obvious enough that it is not worth doing |
| **Moving parts cost more than they look.** | Every extra mechanism is another thing to build, another thing to test, and another thing to be wrong at 2am. We chase serious failures, not every possible one |

This is a real threat model, and it is a reasonable one. It is written here so nobody later "improves" the design against a threat we deliberately chose not to fight.

## A correction to the first draft of this ADR

The first version said *"a drop can never work offline — only a pickup can"*, and built the whole recommendation on it.

**That was wrong**, and it was the load-bearing claim.

The claim was that an offline cabinet cannot look up whose phone number `0909618328` is, so a drop is impossible. That part is true. But it does not follow that the drop must fail. The cabinet can take the number, open a box, and settle up when the network returns. The lookup is a **check**, not the thing that makes the drop possible.

So the true picture is: **both a drop and a pickup can work offline.** Which changes the answer.

## The two failure modes are different, and this is the whole decision

| When the network drops | Which parcel is affected |
| --- | --- |
| **After the drop.** Parcel went in while things worked, network fails later | The cabinet was told about this parcel. It can remember it |
| **Before the drop.** Network already down when the driver arrives | The cabinet was never told anything. It has to cope alone |

A design that only handles the first row leaves a hole. Not a rare hole either — an outage lasting an hour covers both a delivery round and the pickups after it.

## The two options

### Option A — the cabinet is sent the codes in advance

While the network is up, the server sends each cabinet the codes for the parcels currently inside it. Network drops; the cabinet already knows which codes are good.

| Good | Bad |
| --- | --- |
| Almost free — `P5-08` already builds a typed code, this only caches it | **Covers the first row only.** A parcel dropped during the outage has no code, because the server never knew about it |
| No cryptography to design or get wrong | Live codes sit on the cabinet, and must be wiped the moment a parcel leaves |
| Nothing new for a user to learn | The gap is invisible until it bites, and it bites during the exact outage it was built for |

### Option B — challenge and response

The cabinet asks a question. The phone answers it using a secret only it holds. The cabinet checks the answer by itself, with no network.

This is the team's original Google-Authenticator idea. **The shape was right.**

```
   ①  She types her phone number on the cabinet
   ②  The cabinet shows a QR — a one-off random number, nothing more
   ③  Her phone reads it, mixes it with her stored secret, shows 6 digits
   ④  She types the 6 digits
   ⑤  The cabinet works out the same 6 digits and compares
```

| Good | Bad |
| --- | --- |
| **Covers both rows.** The cabinet can check anybody, including a parcel dropped during the outage | The cabinet must hold key material |
| No clock needed — see below | Guessing must be locked out, and that counter must survive a power cut |
| Nothing sensitive on the cabinet screen. The QR is a random number | Real cryptography, even if only a little |
| Her phone needs no network either. The secret is already on it | |

## Decision — recommend Option B

**Reasons, in order of weight:**

1. **A has a hole exactly where it is needed.** It handles the outage that starts after the delivery, and fails the one that starts before it. There is no reason to expect the friendly kind.
2. **Fewer cases to reason about, not more.** A means two behaviours to hold in your head: *"parcels from before the outage work, parcels from during it do not."* B means one: *"it works."* The code is bigger; the thing you have to explain to a user, and debug at 2am, is smaller.
3. **The threat model above allows it.** B's main cost is key material on the cabinet. A guard post plus a low-value target makes that an acceptable trade — deliberately, and written down.
4. **Challenge–response beats TOTP here**, which is a real bonus of the original idea. Because the cabinet supplies a fresh random number each time, the two sides never need agreeing clocks. That removes the single nastiest problem in offline authentication. Google Authenticator needs clock sync; this design does not.

**Option A is not dead.** It is nearly free once `P5-08` exists — the server sends the code to the cabinet as well as the phone. If B slips, A is a one-day fallback that covers most outages. Build B; keep A in the pocket.

## Three things that were wrong in the first sketch

The idea was right. These three details would have broken it, and they are the ones that only show up when someone has been bitten before.

### 1. "Thuật toán có mình biết thôi" — the algorithm will not stay secret

The algorithm ships **inside the app**, on students' phones. An Android app can be taken apart in an afternoon with free tools. One person does it once, posts it, and if the algorithm was the only secret, every box opens.

**Kerckhoffs's principle:** a system must stay safe when everything about it except the key is public. Every cipher in real use is published on purpose, so that thousands of people can attack it and prove it holds.

**Fix:** a **published algorithm** — HMAC-SHA256, the same one Google Authenticator uses, in every standard library — and a **secret key**. The key is the secret. Never the method.

### 2. Encryption is the wrong tool

The sketch said *encrypt* the phone number.

Encryption hides a value from people who should not read it. The phone number is not a secret — the person standing at the cabinet just typed it in. The real question is *"can you prove you hold the secret?"*, and the tool for that is a **MAC**, not encryption.

It matters in practice: a code worked out from the phone number alone can be produced by anybody who knows a phone number and the algorithm. Point 1 says the algorithm becomes known. So the answer must depend on a secret the attacker does not have.

### 3. Six digits is small — so guessing must be stopped

A 6-digit answer is one chance in a million per try. That is plenty **only if guessing is limited**. Unlimited tries at one per second breaks it in under two weeks; ten cabinets in parallel, faster.

**Fix:** a small number of wrong tries locks that box for a while, and **the counter survives a power cut**. Otherwise the attack is: guess, unplug, plug back in, repeat.

## How it works, concretely

Enough detail that `P0-12` is a decision, not another design meeting.

**Setting up**

- The server holds one master key **per cabinet**. Never one key for all of them.
- The cabinet is given its own key at setup, by hand. This is task `P0-05`, which already exists.
- When a student registers, the server works out her secret — `secret = HMAC(cabinet key, her phone number)` — and the app stores it in the phone's secure store. She never sees it.

The point of deriving it this way: **the cabinet stores no user list.** It can work out any user's secret from the phone number typed at the keypad. Nothing to sync, nothing to leak, and a student who registered during the outage still works.

**Picking up, offline**

1. She types her phone number.
2. The cabinet picks a random number, shows it as a QR, and remembers it.
3. Her phone reads it, and works out `HMAC(her secret, random number + box number)`, cut down to 6 digits.
4. She types the 6 digits.
5. The cabinet works out the same value and compares. Match → the door opens.
6. That random number is thrown away. It never works twice.

**Afterwards**

Everything that happened offline is sent to the server the moment the network returns. An outage must never lose the record of who opened what.

## What we accept, on purpose

| We accept | Because |
| --- | --- |
| **The cabinet holds a key.** Someone who opens it and reads the chip could work out every user's secret for that cabinet | Guard post, low-value contents. And the key is per cabinet, so one loss does not spread |
| **A typo during an offline drop puts a parcel in a box addressed to nobody** | Rare — needs an outage and a delivery at the same time. Recovered by staff opening the box. Not worth a mechanism |
| **A borrowed unlocked phone can collect a parcel** | True of every phone app ever built. The phone's own lock screen is the defence |
| **We do not defend against someone with the cabinet's key and physical access** | That is the guard's job, not the software's |

## Non-negotiable, whichever option is chosen

1. No secret algorithm. A published one, with a real key.
2. One key per cabinet. Never one key shared by all cabinets.
3. Every offline code works once, and expires.
4. Wrong tries lock the box, and **the count survives a power cut**.
5. Everything that happened offline reaches the server when the network returns.
6. A wrong code never says whether that code exists. "Not right" and "not right yet" read the same.

## Open questions

1. **How often does campus Wi-Fi actually drop, and for how long?** Nobody has measured it. It does not change the choice — B handles both outage shapes either way — but it decides how much testing this deserves. One week of logging.
2. **Can the cabinet count wrong tries across a power cut?** Needs somewhere to write that survives losing power. Ask the hardware team in the same breath as `P0-01`.
3. **One cabinet or several, in the first release?** With several, a student's phone holds one secret per cabinet. Still fine, but the app has to know which is which. Assumption `A-12`.

## Affects

- `docs/roadmap/phase-0-agree.md` — **P0-12** decides this
- `docs/roadmap/phase-6-faults.md` — **P6-06** pickup offline, **P6-07** drop offline
- `docs/reference/api-contract.md` — the chosen way gets written up there
- `docs/reference/architecture.md` — the offline path is drawn there
- Assumption **A-09** — this ADR only matters because the cabinet is online the rest of the time
