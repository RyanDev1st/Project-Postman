# 0026 — The booking makes the number true. Google makes the account

- **Status:** accepted
- **Date:** 2026-09-02
- **Deciders:** Ryan
- **Supersedes:** [0012](0012-passwords-on-a-phone-account.md)

## Context

[ADR 0012](0012-passwords-on-a-phone-account.md) made the one-time code the only door that creates an account. Every student therefore costs at least one SMS, and the reason given was that the shipper finds a receiver by typing a phone number, so an account with no proved number could never be sent a parcel.

Ryan asked whether a number can be proved without paying for SMS. It cannot, on this campus, in 2026:

- **Silent network authentication** — the carrier confirms the number with no code — is the right answer everywhere it exists. It does not exist here. Twilio's is live in nine countries and Vietnam is not one; Viettel, MobiFone and VNPT signed a GSMA Open Gateway memorandum in April 2025, which is an intent to build, not an API.
- **Firebase phone auth** has had no free tier since September 2024 and needs a card on file.
- **VNeID** is reachable only through C06 and licensed intermediaries, and rules effective 28 September 2026 restrict passing its data onward.
- **Zalo ZNS** is genuinely cheaper — about 300 VND against 600 to 1,000 for SMS brandname — but needs a verified Official Account and the business paperwork behind it.

The way out is not a cheaper channel. It is noticing what the number is for. **The number is a delivery address, not a credential.** Its only job is to let the shipper find the right person. Nothing is unlocked by knowing it, and the door is opened by the app login at pickup — never by the number.

That reframes the risk. A number proved by SMS defends against one thing: somebody claiming a number that is not theirs, so that another person's parcels are routed to them. If the number can only ever be entered by a signed-in account holder, against a parcel they are expecting themselves, then a wrong number breaks nobody's delivery but their own.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| **A** — keep the one-time code as the only door | Nothing changes. The number is proved | Every student costs an SMS, and the whole account rests on a channel we would rather not pay for or depend on |
| **B** — move to Zalo ZNS | Half to two thirds cheaper | Still per-message, still a provider, and it needs business verification. It buys a discount, not a change of shape |
| **C** — Google on the university domain makes the account; the number is claimed inside a booking | No SMS at all. A real name from the school directory. A wrong number harms only the person who typed it | An unverified number can collide with somebody else's, and a student with no working Google account has no way in |

## Decision

We choose **C**. Two changes, and everything else in 0012 stands.

### 1. Google, on the university domain, makes the account

`GoogleTokens.subjectOf` gains an `hd` check beside the `aud` check it already does: the token must carry `hd == "vgu.edu.vn"`. The account is still keyed by `sub` and never by the email address.

| Way in | How | Makes an account? |
| --- | --- | --- |
| Google, on the university domain | `POST /auth/google` | **Yes.** The only one |
| Phone number and password | `POST /auth/password-login` | No. Set on an account that exists |
| One-time SMS code | — | **Removed** |

The one-time code goes. Its only remaining job was proving a number, which a booking now does, and notifications travel by email and app push rather than SMS. The password reset path in 0012 was "sign in with a code, then set a new password"; it becomes "sign in with Google, then set a new password". Same shape, no SMS.

### 2. A booking makes the number true

A signed-in receiver books a box before the parcel arrives, and types their phone number as part of that booking. The number is confirmed on a second panel before it is accepted, because a typo here is the only thing that can go wrong and it is silent when it does.

| Rule | Value |
| --- | --- |
| A booking reserves **one specific box** | The door is held, not just a promise |
| It expires | **24 hours** |
| One live booking per person | A second request is refused while one is open |

### 3. What the shipper sees

The shipper types a number on the cabinet screen, and the answer is one of three:

1. **It matches a live booking.** The reserved box opens. The screen shows the receiver's **masked** name to confirm against the label.
2. **It matches a registered receiver with no booking.** The drop is allowed and `claimFree` takes any free box, which is what the cabinet does today.
3. **It matches nobody.** Before giving up, the screen offers any live booking whose number differs from the typed one by a single digit or a transposition — *"Did you mean Nguyễn V. A***?"* — because a typo by the person who placed the order is the likeliest cause. Failing that, it lists the masked names of people with live bookings, and no numbers. If none of them is the right person, the screen tells the shipper to contact the recipient and to leave the parcel at **ABO**, the grocery store facing the campus back gate.

Names on that screen are masked, as `maskName` already masks them. The shipper is holding a label with the name printed on it, so a mask is enough to match against, and the cabinet is a public terminal that anybody can walk up to — rule 6 in [architecture.md](../reference/architecture.md).

## Why

The number never has to be proved because it can no longer be claimed against somebody else's parcel. A booking is made by an authenticated member of the university, for a delivery they are expecting, and a wrong digit means their own parcel does not match. That is a self-correcting error, not a security hole, and it is the only class of error left once the code path is gone.

## What we accept

- **A number can be claimed twice.** Two students may type the same digits — a transposition, or one typing a parent's number. The first claim wins and the second is refused with "that number is already in use", naming nobody.
- **A student with no working university Google account cannot register.** Every VGU student has one, and somebody who has lost access to it has an IT problem rather than a locker problem.
- **Reservations can starve the cabinet.** Twenty live bookings fill a twenty-box cabinet with nothing inside it. The 24-hour expiry and the one-booking-per-person limit are what bound this. A walk-up drop under case 2 uses genuinely free boxes only, and is refused to ABO when there are none. If this bites in the first week, the answer is to stop holding a specific door and hold only the claim.
- **The near-miss hint is a small oracle.** Somebody at the cabinet can learn that a number close to one they typed is registered. It costs a masked name to a person already standing at the locker with an almost-correct number, and the rate limit on the endpoint is what keeps it from being swept.

## Affects

- **Supersedes** [0012](0012-passwords-on-a-phone-account.md) on account creation. Passwords are unchanged.
- **[api-contract.md](../reference/api-contract.md)** — endpoints 1 and 2 (`/auth/request-code`, `/auth/verify-code`) are removed; 19 (`/auth/google`) becomes account-creating; 10 (the cabinet's lookup) gains the near-miss and the booking match; new endpoints for making, reading and cancelling a booking. **The contract changes first**, before either front-end moves.
- **Schema** — `receivers.phone` becomes nullable, unique only when set. A new `bookings` table.
- **[Phase 2 — Register](../roadmap/phase-2-register.md)** and **[Phase 3 — Shipper drop](../roadmap/phase-3-shipper-drop.md)** both change shape. New task IDs, taking the next free numbers in each phase.
- `Otp.kt`, `Sms.kt` and the SpeedSMS dependency come out, along with the `speedSmsToken` setting and the demo sender that prints codes.
