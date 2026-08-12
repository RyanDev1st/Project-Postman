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

## Closed bugs

| ID | Device + OS | Screen | What happened | Fixed in | Re-tested by |
| --- | --- | --- | --- | --- | --- |
| BUG-001 | Chrome 2026-08-12, headless and desktop | Mock-up — `docs/designs/mockup`, dark mode toggle | The toggle showed the sun in dark mode. The moon never appeared. Measured, the moon's box was 0 by 0 in both states: it is a `<span>` with no `display` rule, so it stayed inline, and width and height do not apply to an inline box. Every other part of the component is blockified by something else. A `<button>` takes phrasing content only, so converting the component's `<div>`s to `<span>`s to make the markup valid deleted the moon in silence. | `display: block` on `.theme-switch__moon`, glass.css | Claude — same headless Chrome. Day moon now measures 21.25px parked at translateX(21.25); night moon 21.25px at identity, covering the sun. Screenshot agrees. |

## Writing a good row

- **What happened** — what you saw, not what you think caused it. "The screen stayed white for 30 seconds."
- **What should happen** — the expected behaviour in one sentence.
- Always record the **device and OS version**. Half of mobile bugs live on one device only.
- If you cannot reproduce it, still log it, and write "seen once".
