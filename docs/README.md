# Docs index

One line per doc. Add a row when you add a doc. Move a row to **Legacy** when the doc is no longer true.

## Roadmap — the plan and the task board

| Doc | What it holds |
| --- | --- |
| [roadmap/README.md](roadmap/README.md) | Master board. Task format, the `Done` line, tick rules, phase counts, current phase |
| [roadmap/phase-0-agree.md](roadmap/phase-0-agree.md) | Settle the stack, the cabinet hardware, the API contract. No code |
| [roadmap/phase-1-skeleton.md](roadmap/phase-1-skeleton.md) | Empty app on both phones, empty screen on the cabinet, both reach the server |
| [roadmap/phase-2-register.md](roadmap/phase-2-register.md) | A receiver registers with a phone number and stays logged in |
| [roadmap/phase-3-shipper-drop.md](roadmap/phase-3-shipper-drop.md) | The cabinet screen. A shipper finds the receiver and a box opens |
| [roadmap/phase-4-notify.md](roadmap/phase-4-notify.md) | The notice arrives and the parcel shows in the app |
| [roadmap/phase-5-pickup.md](roadmap/phase-5-pickup.md) | Scan the cabinet QR, the right box opens. The core feature |
| [roadmap/phase-6-faults.md](roadmap/phase-6-faults.md) | Faulty box, two parcels, nobody collects, shipper walks away |
| [roadmap/phase-7-history.md](roadmap/phase-7-history.md) | What happened, for the receiver and for staff |
| [roadmap/phase-8-ship.md](roadmap/phase-8-ship.md) | Device tests, bug fixing, release build |

## Reference — how the system works **now**

Edit these in place. No date in the name.

| Doc | What it holds |
| --- | --- |
| [reference/working-rules.md](reference/working-rules.md) | **How this team works.** Evidence, small batches, verify, review, restraint. Holds the assumption log |
| [reference/architecture.md](reference/architecture.md) | The parts of the system and how they connect. Two front-ends |
| [reference/api-contract.md](reference/api-contract.md) | Every request each front-end sends and every answer it expects. **Draft** |
| [reference/app-screens.md](reference/app-screens.md) | The phone app: every screen, what leads where, what each must never show |
| [reference/releasing.md](reference/releasing.md) | How a build gets onto the team's phones. Firebase App Distribution |
| [reference/how-it-works.html](reference/how-it-works.html) | **One chart** of the whole process, for anyone. Open it in a browser |
| [reference/glossary.md](reference/glossary.md) | Plain-English meaning of every term used in this repo |
| [reference/bug-log.md](reference/bug-log.md) | Running list of bugs found on real devices |

## ADR — why we chose something

One short file per hard decision. Never edited after it is `accepted`. Reverse it with a new ADR.

| Doc | Decision | Status |
| --- | --- | --- |
| [adr/0000-template.md](adr/0000-template.md) | Template. Copy it | — |
| [adr/0001-tech-stack.md](adr/0001-tech-stack.md) | Phone app stack: Kotlin, native Android | accepted |
| [adr/0002-where-rules-live.md](adr/0002-where-rules-live.md) | Where working rules live, and which file is the authority | accepted |
| [adr/0003-parcel-locker-product.md](adr/0003-parcel-locker-product.md) | Parcel drop-off product. The phone scans the cabinet, not the reverse | accepted |
| [adr/0004-offline-pickup.md](adr/0004-offline-pickup.md) | How a pickup works when the cabinet loses the network | **open** |
| [adr/0005-we-propose-they-object.md](adr/0005-we-propose-they-object.md) | We write the proposal; what we guess, we make adjustable | accepted |
| [adr/0006-no-sensor.md](adr/0006-no-sensor.md) | No sensor. The door closing is the evidence | accepted |
| [adr/0007-android-first.md](adr/0007-android-first.md) | Android MVP first. iOS deferred, not cancelled | accepted |
| [adr/0008-cabinet-simulator.md](adr/0008-cabinet-simulator.md) | A simulated cabinet, and the line it must not cross | accepted |
| [adr/0009-start-phase-1-early.md](adr/0009-start-phase-1-early.md) | Start Phase 1 before Phase 0 is finished. Cabinet screen is a web page | accepted |
| [adr/0010-c4-offline-exception.md](adr/0010-c4-offline-exception.md) | The one case a pickup may happen with no server | accepted |
| [adr/0011-google-sign-in-no-passwords.md](adr/0011-google-sign-in-no-passwords.md) | Google is a second door onto a phone-proved account. No passwords | superseded by 0012 |
| [adr/0012-passwords-on-a-phone-account.md](adr/0012-passwords-on-a-phone-account.md) | Passwords after all, set on a phone account. No email, no reset flow, lockout on disk | accepted |
| [adr/0013-a-real-map-and-its-key.md](adr/0013-a-real-map-and-its-key.md) | A real Google map on Home, its key in gitignored `local.properties`, the drawn plan as fallback | superseded by 0014 |
| [adr/0014-openfreemap-not-google.md](adr/0014-openfreemap-not-google.md) | OpenFreeMap tiles drawn by MapLibre. No key, no account, no card. The baked plan deleted | accepted |
| [adr/0015-language-in-the-composition.md](adr/0015-language-in-the-composition.md) | The language is swapped in the composition, not on the Activity. The platform's own API is API 33, and both platform routes restart the screen | accepted |
| [adr/0016-precise-location-and-live-routing.md](adr/0016-precise-location-and-live-routing.md) | Precise location returns, and the walk is routed live from where you stand. Past 3 km it says the distance instead of drawing a walk nobody is taking | accepted, one row reversed by 0017 |
| [adr/0017-no-line-when-it-is-not-a-walk.md](adr/0017-no-line-when-it-is-not-a-walk.md) | Past 3 km the map draws no line at all — just you and the cabinet, framed together. A red line means "this is your way", and drawing one that is not is the same lie as a wrong caption | accepted |
| [adr/0018-ml-kit-and-play-services.md](adr/0018-ml-kit-and-play-services.md) | The QR reader is ML Kit, bundled model. There is no Google-free build of it, so the pickup path depends on Play Services. No key, no card. zxing-cpp is the named way out | accepted |

## Findings — dated reports

One file per report: `findings/YYYY-MM-DD-<topic>.md`. Never edited after the day. See [findings/README.md](findings/README.md).

| File | What it found |
| --- | --- |
| [findings/2026-08-07-overnight-sweep.md](findings/2026-08-07-overnight-sweep.md) | Every open task chains back to a server address we do not have. P0-04 is the critical path and was wrongly deferred |
| [findings/2026-08-11-otp-delivery-cost.md](findings/2026-08-11-otp-delivery-cost.md) | No route delivers a one-time code for free. SpeedSMS stays, but for a different reason than the spec gives: it is the only one needing no app, account or data |
| [findings/2026-08-13-design-language.md](findings/2026-08-13-design-language.md) | The app reads cheap because nothing in it agrees with anything else: 42 spacing values, 12 radii, a title set smaller than body text. And `ink3` fails WCAG AA in both schemes |

## Specs — feature design docs before they are built

| Doc | What it holds |
| --- | --- |
| [superpowers/specs/2026-08-09-otp-sender-design.md](superpowers/specs/2026-08-09-otp-sender-design.md) | The OTP sender server: contract endpoints 1–4, SpeedSMS + demo providers, code/token lifecycle |

## Legacy — docs that are no longer true

Moved here, never deleted. See [legacy/README.md](legacy/README.md).

| Doc | Why it is here |
| --- | --- |
| [legacy/roadmap-student-locker/](legacy/roadmap-student-locker/) | The first board, for a student storage locker. The product changed on 2026-08-05, before any task was ticked. See [ADR 0003](adr/0003-parcel-locker-product.md) |
