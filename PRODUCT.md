# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

*Inferred, not confirmed.* The receiver's phone app is the product; the cabinet screen is a supporting terminal built as a web page ([ADR 0009](docs/adr/0009-start-phase-1-early.md)). iOS is deferred, not cancelled ([ADR 0007](docs/adr/0007-android-first.md)).

## Users

Two users, on two different surfaces. **They never meet, and neither sees the other's screen.**

**The receiver** — a VGU student or staff member expecting a parcel. Uses the phone app. Opens it roughly twice per parcel: once when the notice arrives, once standing at the cabinet. Most sessions are under twenty seconds, one-handed, sometimes walking. The account is a phone number and nothing else.

**The shipper** — a delivery courier (SPX, Shopee and similar) dropping a parcel off. Uses the cabinet's own touchscreen. No account, no app to install, no login. Working fast, often with a parcel in one hand, at a screen anyone can walk up to and read.

## Product Purpose

A parcel drop-off locker for VGU. A courier leaves a parcel in a box; the receiver collects it with their phone. **Nobody needs a key, and the two never have to meet or agree a time.**

Success is a courier leaving a real parcel and a student collecting it, with no staff involved and nothing handed over in person.

## Positioning

**The phone scans the cabinet, not the other way round.**

The QR shown on the cabinet screen says only *which cabinet, at what moment*. It is not a key. Identity comes from the app login, so a photograph of the cabinet screen opens nothing. A neighbouring design that puts the code on the parcel or the phone screen cannot make that claim — there, the code is the key, and anyone holding a picture of it holds the parcel. Decided in [ADR 0003](docs/adr/0003-parcel-locker-product.md).

## Operating Context

- **VGU campus, Vietnam.** Couriers are local and Vietnamese-speaking. Parcels arrive with SPX and Shopee labels.
- **The cabinet:** 1800 × 1600 × 450 mm, doors numbered 01–20. Door 06 is the control panel, so **19 boxes actually hold parcels** (assumption A-15, unconfirmed).
- **Network:** the cabinet connects over campus Wi-Fi (P0-01).
- **No sensor in the boxes.** A door closing is what records a delivery ([ADR 0006](docs/adr/0006-no-sensor.md)). A courier can therefore open a door, walk off still holding the parcel, and the system will believe it was delivered. Traceable afterwards, not preventable beforehand, and accepted knowingly.
- **No real cabinet exists yet.** A Blender simulator stands in for it and is **never evidence** ([ADR 0008](docs/adr/0008-cabinet-simulator.md), task P0-16).
- **The Server team owns the API and the database**, in a separate repository. This repo calls that API and changes nothing on it.

## Capabilities and Constraints

**Built so far** — phone app: Kotlin, Jetpack Compose, native Android, `minSdk` 24, `compileSdk`/`targetSdk` 36, package `vn.edu.vgu.smartlocker`. Cabinet screen: a plain web page.

**The cabinet screen may never assume a screen size.** Nobody can say what hardware it runs on, and that question is deferred ([ADR 0009](docs/adr/0009-start-phase-1-early.md)). It is proved at 1280 × 800 and 1920 × 1080 as a stand-in.

**Safety rules, none of which are style choices:**

- A door opens only from a direct tap or scan — never from a retry, a timer, or a screen coming back into view. One tap is at most one open request.
- An unclear result is never shown as a success.
- The cabinet screen is a public terminal: never a full name, never a phone number, never a parcel list.
- The typed pickup code is 6 digits, works once, lives 48 hours, and the box locks for 15 minutes after 5 wrong tries. A wrong code must never reveal whether that code exists.
- No key, token or private address in this repository or in either build. The cabinet key is placed on the device at setup.

**Explicitly undecided:**

- Whether the cabinet may open a door with no server involved. Task P0-12 chooses challenge-and-response; task P0-13 must first settle whether that breaks the rule that the front-end never decides.
- The cabinet's screen size, its operating system, and how the cabinet proves it is the cabinet. All deferred, all recorded on the board.
- Every guessed number lives in `config/settings.json` so correcting one costs an edit, not a release.

## Brand Commitments

**Name:** VGU Smart Locker. In Vietnamese, *Tủ gửi hàng thông minh*.

**Bilingual, English and Vietnamese, on every screen.** *Which language leads is not decided.* The code currently leads in English. Worth resolving before the copy is written: the receiver studies at an English-and-German university, but the shipper is a courier who may read no English.

**Visual direction — given by the user on 2026-08-06, binding.** Recorded as stated, not expanded:

> Depth, "liquid glass", modern, a 3D feel. Minimal — far fewer components, low bloat, focused. Strong conversion. UX-led, with everything intentional.

References supplied: `mobbin.com/discover/apps/ios/latest`, and the **Brick** app on Mobbin — described as aesthetically minimal with strong conversion.

*Noted as a fact, not resolved here:* both references are iOS, and "liquid glass" is Apple's own design language. This product is Android. Reconciling the two is a visual-world decision, not a product one.

**Voice:** plain words, short sentences, written so somebody with no technical background can follow. The project writes its own documents this way and the interface follows.

## Evidence on Hand

Real, in this repository:

- `docs/reference/app-screens.md` — the seven app screens, the flow between them, and what each must never show
- `docs/reference/api-contract.md` — the full API proposal, 15 endpoints. **Written, not yet sent to the Server team**
- `docs/reference/architecture.md`, `docs/reference/working-rules.md` — including an assumption log, A-01 to A-18
- `docs/adr/` — nine decision records
- `docs/roadmap/` — 75 tasks, 11 done, every tick carrying written evidence
- A working Android build, `v0.1.0`, and a cabinet-screen skeleton in `src/cabinet/`
- `scripts/cabinet-sim/` — the Blender cabinet. **Never evidence**

**What does not exist, and must never be invented:** real users, real parcels, a real cabinet, a running server, usage numbers, testimonials, or any deployment claim. The app has not yet been seen running on a phone.

## Product Principles

1. **Decide from evidence, not assumption.** Nothing is "done" without saying what was run and what came out. A simulator can drive the work; it can never close a task.
2. **The phone proves who you are; the cabinet proves only where you are.** Every security decision follows from that split.
3. **The public terminal knows the least it can.** If a stranger reading the cabinet screen learns something about a person, that is a defect.
4. **Nothing is a dead end.** Every failure offers the next thing to try, and the typed code is the floor under all of it.
5. **Guessing is fine; guessing invisibly is not.** Every guessed number lives in one settings file, and every assumption is written down with a way to check it.

## Accessibility & Inclusion

- **The app:** one hand, standing, sometimes walking. Anything tappable sits in the lower two-thirds of the screen.
- **The cabinet:** corridor light, a courier in a hurry, a parcel in one hand. Large type, high contrast, big touch targets.
- **Both:** English and Vietnamese, with correct Vietnamese diacritics — the tone marks are not decoration.
- The cabinet layout must stay readable from a small tablet up to a large monitor, because nobody yet knows which it will be.
