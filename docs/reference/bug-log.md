# Bug log

Every bug found on a real device. Required by the team requirements: *"test the app on Android/iOS, log every bug and fix it."*

## How to use

Add a row the moment you see a bug. Do not wait until you understand it.

**Status:** `open` → `fixed` → `re-tested`. A bug is closed only at `re-tested`, on the same device that found it.

**ID:** `BUG-001`, in order. Use it in the commit: `fix(BUG-007): stop crash on airplane mode`.

## Open bugs

| ID | Device + OS | Screen | What happened | What should happen | Status | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| BUG-002 | Ryan's phone, Android | Settings — dark mode toggle | Faint circles spread out around the knob and sit on the screen behind the toggle. Ryan: "awkward layered fading circles around the moon". | The circles are the design's own — four white 10% rings around the knob — but the pill clips them, so they only lighten the sky inside the pill and never appear outside it. | fixed — the knob now draws inside the track's clip, and the extra 20% disc over the well is gone. Seen in `build/parity/theme-switch.png`. Needs re-testing on Ryan's phone | Claude |
| BUG-003 | Emulator `parity`, Android 16, 780x1690 @320dpi, software GL | Settings — dark mode toggle | The switch is a speckled grey blob. The knob has no clear shape and the track is dusted with light specks. The two plain toggles above it on the same screen — Parcel arrived, SMS backup — are clean, so it is this component and not the screen. Seen in `v080e-settings.png` at 0.8.0. | The switch reads as one pill with one round knob, the way the mock-up's `.theme-switch` does. | open — **and now probably not a component fault.** 2026-08-13, the parity loop (P1.5-01) drew this component on this machine in the dark scheme, both states, and it came out clean: sun and clouds on one, moon and stars on the other, no speckle. `build/parity/theme-switch.png` beside the reference. `ThemeSwitch.kt` uses no `RenderEffect` and no AGSL — only `setShadowLayer`, which both renderers really run — so the geometry and colours are right and the speckle is the emulator's software rasteriser on a blurred shadow. Same class as BUG-004. Needs one look on a real phone to close | — |
| BUG-004 | Emulator `parity`, Android 16, 780x1690 @320dpi, software GL | Sign in — the cabinet hero | The render fills only the left third of its box, about 124dp of a 358dp box, and stops. `AuthHero`'s own comment says the window should show image fractions x 0.46–1.03, so the render should reach within 3% of the right edge. Held for 40 seconds in case it was a slow first draw; unchanged. Seen at 0.9.1 in `after/SIGNIN.png`. | The render reaches the right edge of its box, then goes transparent so the ground shows through. | open — **not caused by the token pass**: that change moved this screen's gutter 17dp to 16dp, which moves the render's width by about 3dp, not 200. Suspected swiftshader failing to scale a 1250px bitmap. Needs a look on a real phone before anyone changes code | — |

## Closed bugs

| ID | Device + OS | Screen | What happened | Fixed in | Re-tested by |
| --- | --- | --- | --- | --- | --- |
| BUG-005 | This machine, JVM render (`DesignParityTest`) — no phone | Every screen, both schemes | Two colour tokens were mistyped, and each one's own comment recorded the right number. Dark `hair` was `0x211EBAE8` — red 30 where the design says 150 — so the hairline edge on every card, field, recess and glass pane in dark mode was drawn teal instead of pale blue. Light `shadow` was `0x38181C24`, giving (24,28,36) where the design says (12,24,36), so the light-mode card shadow was pale and short of blue. Found by comparing all 46 colour tokens against `screens.css` by number, not by eye. | `Color.kt` — `0x2196BAE8` and `0x380C1824`, taken from `--hair: rgba(150,186,232,.13)` and `--shadow: rgba(12,24,36,.22)` | Claude — same JVM render. All 46 tokens re-checked against `screens.css`: every one agrees except `ink3` in each scheme, moved on purpose to reach WCAG AA. Seen in `build/parity/card.png` and `card-light.png` |
| BUG-001 | Chrome 2026-08-12, headless and desktop | Mock-up — `docs/designs/mockup`, dark mode toggle | The toggle showed the sun in dark mode. The moon never appeared. Measured, the moon's box was 0 by 0 in both states: it is a `<span>` with no `display` rule, so it stayed inline, and width and height do not apply to an inline box. Every other part of the component is blockified by something else. A `<button>` takes phrasing content only, so converting the component's `<div>`s to `<span>`s to make the markup valid deleted the moon in silence. | `display: block` on `.theme-switch__moon`, glass.css | Claude — same headless Chrome. Day moon now measures 21.25px parked at translateX(21.25); night moon 21.25px at identity, covering the sun. Screenshot agrees. |

## Writing a good row

- **What happened** — what you saw, not what you think caused it. "The screen stayed white for 30 seconds."
- **What should happen** — the expected behaviour in one sentence.
- Always record the **device and OS version**. Half of mobile bugs live on one device only.
- If you cannot reproduce it, still log it, and write "seen once".
