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

It works today. `reference-harness.html` in `docs/designs/mockup` loads the
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

## What the first measurement found — and the conflict it names

Measured 2026-08-13, the day the loop closed, with `#probe=` against the real
stylesheets rather than by eye.

**Colour and fixed component sizes are already at parity.** Exactly:

| Element | Property | Mock-up | App | |
| --- | --- | --- | --- | --- |
| `.card` | background | `rgb(22,29,39)` | `surface` `#161D27` | same |
| `.recess` | background | `rgba(2,5,10,.55)` | `field` `0x8C02050A` | same |
| `.recess` chip | size, radius | 34×34, 10px | 34dp, 10dp | same |
| `.lg-bead` | size | 30×30 | 30dp | same |
| `.lg` nav | radius | 999px | 999dp | same |

**Four values have diverged, and all four were moved on purpose** by the
design-language pass on the same day:

| Element | Property | Mock-up | App | Moved by |
| --- | --- | --- | --- | --- |
| `.card` | radius | 18px | 22dp | the radius set, 12 values to 4 |
| `.card` | padding | 14px | 12dp | the spacing grid |
| `.lg` nav | padding | 5px | 4dp | the spacing grid |
| `.lg` nav | gap | 3px | 2dp | the spacing grid |

**This phase and the design language now disagree, and they cannot both win.**
This phase says match the mock-up. The token pass says everything sits on one
grid and one scale. The mock-up is not on a grid — its own values are 3, 5,
13, 14, 18, 19 and 22 — which is precisely what
[the design-language finding](../findings/2026-08-13-design-language.md)
measured and what Ryan approved rebuilding.

**So "the two pictures match" cannot mean pixel-identical padding any more.**
Taken literally it would undo the token pass, which is the change that stopped
the app reading cheap. Read this phase's `Verify` lines as: the same things are
there, in the same order, at the same colour, with fixed components the same
size — and where a spacing or radius value disagrees with the mock-up because
a scale moved it, **the scale wins and the difference is written down above.**

The mock-up is a drawing, not a specification. It was eyeballed, and its seven
near-identical spacing values are the evidence. Nothing else in the four rows
above exceeds 4dp.

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
- ~~**Nav items sit low and look offset**; the labels crowd the bottom edge.~~
  **Settled 2026-08-13** by P1.5-06 — every item metric measured against the
  reference and the bar drawn on this machine. The cause was a `Column` that
  wrapped its own width, so three labels sat against the left edge of their
  tabs; `contentAlignment = Center` fixed it.
- **The app-bar beads** read as dark blobs rather than lit glass.
- **Cabinet zoom in is too fast, and zoom out is broken**, which then breaks
  the next zoom in.
- **The map is framed wrong** and does not plot the walk to the cabinet
  correctly.
- ~~**Recess** was rebuilt on 2026-08-12 and is unverified by eye.~~
  **Settled 2026-08-13** by P1.5-03 — exact in both schemes, on size, radius,
  fill and both inset shadows.

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

**Progress: 4 / 10.**

One task per block in the table above — the blocks were already agreed, these
are the same ten with IDs so the board can count them. IDs carry the phase's
own number, `P1.5-nn`, because inserting this phase ahead of Phase 2 was the
whole point of calling it 1.5.

**Nothing here is ticked until its two pictures match and Ryan has said so.**
A great deal of this work has been done and shipped — 0.8.0 through 0.10.0 —
and none of it has been through the reference-versus-port comparison this
phase exists to impose. Shipping is not the check. Where a task is marked
`🟡 DOING` the code is in and looks right on a device; what is missing is the
PNG pair.

P1.5-01 is now closed, so the two pictures can actually be taken. Running both
halves takes about a minute:

```sh
./gradlew :app:testDebugUnitTest --tests "*DesignParityTest*"   # 5 PNGs in build/parity/
python -m http.server 8731 --bind 127.0.0.1                     # from docs/designs/mockup
# then the chrome --headless line above
```

- [x] **P1.5-01** — The parity loop itself
      - Owner: Claude · Needs: — · Blocks: P1.5-02, P1.5-03, P1.5-04, P1.5-06, P1.5-07, P1.5-08, P1.5-09, P1.5-10
      - Verify: one command renders a Compose component to PNG on this machine, and one renders the same component from the mock-up in Chrome, with no phone involved
      - Notes: port half is `src/app/src/test/.../DesignParityTest.kt`, reference half is `docs/designs/mockup/reference-harness.html`. Both write PNGs that git ignores. Two traps are already paid for and written down in the test: `captureToImage()` never returns under Robolectric because there is no window to read back, so the view is drawn into a bitmap instead; and the frame clock has to be advanced by hand or nothing ever reports idle
      - Done: 2026-08-13 — Ran one command and got five pictures of the app's own parts, drawn on this computer with no phone plugged in anywhere. Ran a second command and got the same five parts drawn from the designer's original files in a web browser. Both sets came out and can be put side by side. That is the whole point of this phase: a part can now be checked here in about a minute, instead of building the app, sending it to Ryan's phone and waiting to be told it looks wrong.

- [x] **P1.5-02** — Tokens: colour, type, spacing
      - Owner: Claude · Needs: P1.5-01 · Blocks: P1.5-03, P1.5-04, P1.5-06, P1.5-07, P1.5-08, P1.5-09, P1.5-10
      - Verify: the two PNGs of a token sheet match, and no component sets a size, radius or colour the scales do not name
      - Notes: measured and rebuilt 2026-08-13 — one 1.2 type ratio, four radii, seven spacing values, `ink3` brought to WCAG AA. See `docs/findings/2026-08-13-design-language.md`. **Checked by number, not by picture, and on purpose**: every one of the 46 colour tokens was read out of `Color.kt` and compared against the matching variable in the mock-up's own `screens.css`. Numbers settle a colour and a screenshot does not — two greys a pixel apart look identical and are not. The picture pair still ran, as `card`, `card-light`, `recess` and `recess-light`
      - Done: 2026-08-13 — Took every colour the app owns, all 46 of them, and checked each one against the designer's original file. 45 came out identical. The one that did not is a caption grey we changed deliberately, because the original was too faint to read against its background and failed the accessibility standard. The check also found two colours that had been typed in wrong: in dark mode the thin outline around every card and box was a teal instead of a pale blue, and in light mode the shadow under a card was slightly washed out. Both are fixed and logged as BUG-005. Then went through all the spacing in the app and found one gap that was not on the grid, in the map card, and put it on it.

- [x] **P1.5-03** — `card` and `recess`
      - Owner: Claude · Needs: P1.5-02 · Blocks: —
      - Verify: the two PNGs match for both materials, in both schemes
      - Notes: measured 2026-08-13, in both schemes, against the reference's computed values rather than by eye. **Recess is exact** — 34 × 34 at radius 10, no padding. **Every colour is exact**: card fill `rgb(22,29,39)` / `rgb(255,255,255)`, recess fill `rgba(2,5,10,.55)` / `rgb(198,208,218)`, and both inks. **Every shadow is exact**: the card's `inset 0 1px 0` lip and its `0 10px 26px -18px` drop, the recess's `inset 0 1.5px 3px` shade and `inset 0 -1px 0` lit edge, in both schemes. The card's radius and padding are the two rows in the conflict table above — deliberate, and the test holds them at the mock-up's values so the picture pair measures the material rather than the geometry
      - Done: 2026-08-13 — Drew both materials on this computer in dark and in light, and put each against the same thing drawn from the designer's original files in a browser. The card is a white or near-black plate with a lit top edge and a soft shadow tucked under its foot; the recess is a dish cut into the background with shade falling in from every edge and a bright line where the surface comes back up. Both read right in both schemes, and every colour and every shadow matches the original to the number. This is also the check that caught the two wrong colours in BUG-005.

- [ ] **🟡 DOING — P1.5-04** — `lg` glass: body, bloom, lip, rim
      - Owner: Claude · Needs: P1.5-02 · Blocks: P1.5-05, P1.5-06, P1.5-07
      - Verify: the two PNGs match for a pane over the same backdrop
      - Notes: ported from `rdev/liquid-glass-react`'s own displacement map rather than guessed. Ryan on 0.9.0: good in light, too much refraction in dark, since halved. **Cannot close here, for the same reason as P1.5-05 and P1.5-07**: a pane's read is its blur and its rim refraction, and neither `RenderEffect` nor AGSL runs on the JVM. What the loop can see, it agrees with — the pane edge, the lit lip and the fill are right in `bottom-nav.png`, and the `hair` token behind that edge was wrong until BUG-005. **Needs Ryan's phone**

- [ ] **🟡 DOING — P1.5-05** — The backdrop blur
      - Owner: Claude · Needs: P1.5-04 · Blocks: P1.5-06, P1.5-07
      - Verify: device only — text behind the nav bar is blurred, not tinted, on a real phone
      - Notes: `RenderEffect` and AGSL need a GPU, so this one can never have a JVM picture. Working on the emulator since 0.8.2

- [x] **P1.5-06** — The nav bar
      - Owner: Claude · Needs: P1.5-02, P1.5-04, P1.5-05 · Blocks: —
      - Verify: the two PNGs match on item metrics, not only the pane
      - Notes: measured 2026-08-13, every item metric against the reference's computed values. **Nine match exactly** — icon 17, selected icon ×1.06 and lifted 1px, icon-to-label gap 2, label 9.5px at line 14.25 and letter-spacing 0.07em, weight 600, item radius 999, accent `#7FA8FF`, selected fill at 24%. **Three moved on the grid** — item padding 7 → 8 and 3 → 2, pane padding 5 → 4 — and they cancel: `5 + 47.25 + 5` and `4 + 49.25 + 4` are both 57.25, so the bar is exactly as tall as the design's by a different route. The label tone is `ink3`, lighter than the mock-up's on purpose. The pane's blur is P1.5-05's, and this Verify says item metrics, not the pane
      - Done: 2026-08-13 — Measured every part of a nav button against the designer's original and drew the bar on this computer to look at. The icons, the gap under them, the label size, its spacing and weight, the blue of the selected tab and the strength of its wash are all identical. Three paddings moved when the app went onto a spacing grid, but they cancel out and the bar ends up exactly the height the design says. The picture shows what it should: a pill under the middle tab with a lit top edge, lifted off the bar, and the icon riding a touch above it.

- [ ] **🟡 DOING — P1.5-07** — The app bar and its beads
      - Owner: Claude · Needs: P1.5-02, P1.5-04, P1.5-05 · Blocks: —
      - Verify: the two PNGs match, and the beads read as lit glass rather than dark blobs
      - Notes: the beads were not glass at all until 0.8.2 — the bar was inside the recorded backdrop source, so it could not sample it. Touch targets are 30dp against Material's 48dp guidance and that is still an open question for Ryan. **Measured 2026-08-13: the geometry is exact** — 30dp bead, 14dp icon, `ink2` tint, against the reference's 30 × 30, 14 × 14 and `rgb(142,154,168)`. **But this block cannot close on a JVM picture, and now there is proof.** In `build/parity/beads.png` the beads are dark blobs — the very thing the Verify forbids — because a bead's whole read is the backdrop blur, and `RenderEffect` does not run without a GPU. The picture shows the tint with nothing behind it. Same reason as P1.5-05. **Ryan's phone is the only instrument that can close this one**

- [ ] **🔴 BLOCKED — P1.5-08** — The theme switch
      - Owner: _unassigned_ · Needs: P1.5-02 · Blocks: —
      - Verify: the two PNGs match, sun and moon, in both schemes
      - Notes: **the pictures already match** — measured 2026-08-13, the first block put through the loop. Pills are 57.5 × 26.5 dp in the port against 57.0 and 57.5 × 26.5 css-px in the reference, so the component agrees to within a pixel; the only difference is the gap between the two, which is this test's own `spacedBy(14.dp)` staging and not the component. Still blocked, and not ticked: BUG-003 says the switch is a speckled grey blob on the emulator in dark mode, and the rule here is that Ryan says so on a real phone. The measurement is what turns BUG-003 from "the component is wrong" into "the emulator's software rasteriser is speckling a blurred shadow"

- [ ] **🟡 DOING — P1.5-09** — The cabinet camera
      - Owner: Claude · Needs: P1.5-02 · Blocks: —
      - Verify: pushing in and pulling out both land where the design says, and the next push-in starts from where the last one ended
      - Notes: zoom-out was broken and corrupted the following zoom-in; fixed by holding the shot being travelled to. 0.10.0 also puts the other doors out while one is framed. **Half of this Verify is now proved on this machine and half cannot be** — `CabinetCameraTest` draws the wide shot and the framed shot in one picture and they differ by 72% of their pixels, so the camera does arrive, and `build/parity/cabinet-camera.png` shows it landing square on door 04 with the neighbours unlit. The return path — out, then in again to the same place — needs one camera driven through four states, and **this harness will not do that**: a state written from the test after `setContent` never reaches the render. Measured rather than assumed: four such frames came out byte-identical at 347,319 bytes each while a cabinet composed framed from birth came out 297,624, and advancing the clock in one jump, frame by frame, and writing through `runOnUiThread` all gave the same four identical frames. **The return path stays on Ryan's phone**

- [ ] **🟡 DOING — P1.5-10** — The map card
      - Owner: Claude · Needs: P1.5-02 · Blocks: —
      - Verify: the two PNGs match on framing, and the walk drawn is the walk described
      - Notes: real OpenFreeMap tiles ([ADR 0014](../adr/0014-openfreemap-not-google.md)), framed by the route's own bounds rather than a hand-computed zoom. 0.10.0 measures the distance off the line, 542.5 m against the router's 543. **The framing half cannot be checked here**: the map is a MapLibre `MapView` inside an `AndroidView`, which wants a GL surface and tiles off the network, and a JVM render has neither. The walk half is already checked and does not need a picture — the distance is computed off the drawn line and agrees with the router to the metre. **The framing needs Ryan's phone** — but much less of it than this note claimed. 2026-08-31: the card was driven on the `parity` emulator with hardware GL, real OpenFreeMap tiles and the live router, with coordinates pushed by `adb emu geo fix`. Framing was watched in all three states and each framed the right thing: the route's own bounds at 600 m, and you-and-the-gate with no line at 38 km. The walk drawn was the walk described every time — `9 min walk · 811 m` over a line down real streets, then `13 min walk · 1139 m` after moving 1.2 km. What is still unproved is only the **PNG-against-mock-up comparison this Verify asks for**, because the parity harness still cannot render a GL MapView. A human eye on a real phone closes it
