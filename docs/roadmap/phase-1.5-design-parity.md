# Phase 1.5 — design parity with the mock-up

Parent: docs/roadmap/README.md

## Why this phase exists

The app has been ported from `docs/designs/mockup/` three times and has been
wrong three times. Ryan, 2026-08-12: *"you are trying so hard but everything
is still broken. I think you should rebuild block by block with the artifact
example in mind so to keep it identical."*

## The actual cause

Not carelessness and not the wrong numbers. **There is no feedback loop.**

Every port so far has been: read the CSS → imagine the result → write Compose
→ ship a build → Ryan installs it → Ryan says it is wrong. That loop is about
twenty minutes long, runs at most a few times a day, and its only instrument
is a person's eye on a phone. Nothing in it can catch "the gradient runs to a
hardcoded 200px", "the frost is near-black so it darkens instead of frosting",
or "the halo is three concentric circles". Those are all things a picture
would have shown in one second.

So the first task in this phase is not a screen. It is the loop.

## The loop

Two halves, both of which run on this machine with no phone:

1. **The reference.** The mock-up is HTML and CSS. Open it in Chrome, screen
   shot each component at a known device size, and keep the PNGs. That is
   ground truth, and it is what "identical" is measured against.
2. **The port.** Render the Compose composables to PNG on the JVM with
   Robolectric, so a component can be looked at without a device, an install,
   or Ryan.

Then a block is done when its two pictures match, and *only* then does it go
in a build.

### Running the reference half

It works today. `scratch_reference.html` in `docs/designs/mockup` loads the
real `screens.css`, the real `glass.css` and the real `liquid.js`, and holds
the components in the markup they have in `body.html`. Serve the folder, then:

```sh
python -m http.server 8731 --bind 127.0.0.1        # from docs/designs/mockup
chrome --headless --disable-gpu --no-sandbox \
  --user-data-dir=<a writable dir> \
  --screenshot=<an absolute path> \
  --window-size=390,860 --hide-scrollbars \
  --force-device-scale-factor=2 --virtual-time-budget=5000 \
  "http://127.0.0.1:8731/scratch_reference.html"
```

Two things the harness does that a plain screenshot does not:

- `#block=<n>&zoom=<n>` keeps one component and blows it up, so it can be
  looked at at pixel scale.
- `#probe=<css selector>` prints each match's box and its computed transform,
  colour, radius, shadow, padding and type as JSON. **Prefer this to looking.**
  It is what found BUG-001 — a screenshot showed a sun in dark mode and left
  the reason open, while the probe said `box: [.., .., 0, 0]` and named it.

Two traps, both already paid for: Chrome will not write a screenshot to a
relative path in the session scratch directory, so pass an absolute one; and
the harness disables every transition, because a reference is a still and the
first capture caught the moon half way across the sun.

**What this cannot check:** `RenderEffect` and AGSL do not run without a GPU,
so the backdrop blur and the rim refraction will not appear in a JVM render.
Those two stay device-checked. Everything else — layout, padding, colour,
typography, the shape of a knob, where a label sits — is caught here.

## The blocks, in order

Each block is done when the two PNGs match and Ryan has said so.

| # | Block | Why this order |
| --- | --- | --- |
| 1 | The loop above | Nothing after this is verifiable without it |
| 2 | Tokens — colour, type, spacing | Every block below reads them |
| 3 | `card` and `recess` | The two materials most of the app is made of |
| 4 | `lg` glass — body, bloom, lip, rim | The nav and every bead |
| 5 | The backdrop blur | Device-checked. The glass is a dark tint until this works |
| 6 | The nav bar | Item metrics, not just the pane |
| 7 | The app bar and its beads | The two controls Ryan calls "awkward" |
| 8 | The theme switch | Ported once, and the knob is wrong |
| 9 | The cabinet camera | Timing and the zoom-out path |
| 10 | The map card | Framing and the route |

## What is known wrong right now

Recorded so a rebuild does not lose them.

- **Glass reads as a dark tint.** The frost is `ground` at 55% — near-black in
  dark mode. It only works as frost if the blur behind it works, and the blur
  is not working on Ryan's phone.
- **The theme switch knob** has "awkward layered fading circles" — concentric
  translucent halo circles that the component does not have.
- **Nav items sit low and look offset**; the labels crowd the bottom edge.
- **The app-bar beads** read as dark blobs rather than lit glass.
- **Cabinet zoom in is too fast, and zoom out is broken**, which then breaks
  the next zoom in.
- **The map is framed wrong** and does not plot the walk to the cabinet
  correctly.
- **Recess** was rebuilt on 2026-08-12 and is unverified by eye.

## What is coming, so the rebuild does not have to be undone

The design work has to survive these, all of which are already on the board or
in an ADR:

- The real Server-team API replaces every made-up parcel — screens need
  loading, empty, and refused states, not just the happy one.
- Scan-the-cabinet QR, which is a camera screen over the top of this chrome.
- Push notices for a parcel arriving.
- Password and Google sign-in screens (P2-08, P2-09), which use the same
  field recess and the same buttons.
- Vietnamese and English, which changes every string length and therefore
  every layout that was tuned to English.

Practically that means: no fixed pixel sizes where a token would do, no
composable that only handles the state it was drawn in, and materials that
take their colours from `LockerTokens` rather than from constants.

## Tasks

Numbered when this file is added to the board. Nothing here is ticked until
its two pictures match.
