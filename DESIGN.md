# Design

The phone app's visual system, written from what is built. Values here are in the code or in a render, and were seen on a screen.

Product truth is [PRODUCT.md](PRODUCT.md). Screens and their rules are [docs/reference/app-screens.md](docs/reference/app-screens.md). This file is the *how it looks*, and only that.

**This file follows the design.** It is a record, not a rulebook. When Ryan changes direction it is rewritten in the same session — twice already. An argument this file made and lost is at the bottom under *What this replaced*, because knowing what was tried is worth keeping and re-litigating it is not.

## The one idea

**Show the receiver the door.** Not a number standing for a door — the door, on the real cabinet, lit up, with the screen pushing into it.

A receiver opens this app about twice per parcel, under twenty seconds, one-handed, sometimes walking. They are answering one question: *which one of those metal doors is mine?* The app answers with a picture of the actual cabinet.

Everything else is a list, kept quiet, so the cabinet is the only thing that is ever large.

## Layout comes from the drafts

The layouts follow Ryan's Stitch drafts, and where this file and those drafts disagree, **the drafts win**. What is written here is why each one is arranged the way it is, not permission for it to be.

Three tabs: **Home**, **Cabinet**, **Settings**.

| Screen | Arrangement |
| --- | --- |
| **Sign in** | The cabinet, cropped and bleeding off three edges. Then three lines of 40px type with the last word in the accent. Then the form, on no panel at all |
| **Home** | Greeting, ready count, a card per waiting parcel, then **Collected** underneath. History is a section here, not a tab — it is glanced at, not visited, and on a Home with nothing waiting it is what stops the screen looking broken |
| **Cabinet** | The render, filling the middle. Status pill floating over it. One button |
| **Settings** | Account card, then grouped rows: account, notices, app |

Rules that hold everywhere:

- **Everything tappable sits in the bottom third.** One hand, standing, possibly holding a parcel.
- **The primary button is full width and at least 52dp tall.**
- **A card's leading element is the box number**, set in mono on a tinted chip. There is no photograph of a parcel to show, and the number is the one thing the receiver carries to the cabinet.
- **The nav floats over content**, and content scrolls under it. A glass bar with nothing behind it has nothing to refract.
- **No search on Home.** A receiver has nought to two parcels.

## Getting in

**Three ways, and SMS is not the privileged one.**

| Way | Why |
| --- | --- |
| Phone number, then a code | Works for anyone. **Every message costs money**, on each sign-in and each resend — a real line item for a student project |
| Google | Free, and every student already has an account |
| VGU account | Free, and it proves the person is a student, which SMS never does |

**Apple sign-in is not offered.** Android-first by [ADR 0007](docs/adr/0007-android-first.md); on the target device it is a button nobody can press.

**Social sign-in costs one extra screen.** The shipper finds a receiver by typing a *phone number* on the cabinet. Sign in with Google and the app has an email, so a second screen asks for the number and says why in one line. That is a contract change — see [api-contract.md](docs/reference/api-contract.md).

### The way in has no panel

All three screens — sign in, the code, the phone number — used to put every control inside one rounded card: field, primary button, divider, two more buttons, legal links. Six things of four ranks in one bin, on a single 11px gap. Two faults, both structural:

- **The container was doing the job proximity should do.** Take the card away and nothing said what grouped with what. That is the test of whether a container earns its place, and it failed it.
- **It made four ranks look like one.** The receiver had to *read* three buttons to learn that one costs this project money on every tap and two are free.

The card is gone. Rank is carried by the surfaces, which already differ — a recessed field, a filled button, a hairline-outlined pair — and by three intervals instead of one: **8px** inside a field group, **12px** from a field to its button, **22px** between ranks.

| Was | Now | Why |
| --- | --- | --- |
| A rounded square with a glyph | The cabinet, cropped, bleeding off three edges | A logo placeholder on a product whose identity is a piece of metal in a corridor. The render was already in the page |
| Placeholder as the label | `PHONE NUMBER` above the field, permanently | A placeholder leaves exactly when the field is in use |
| `Continue` | `Send code` | An SMS is about to arrive. The control should say so |
| Three stacked `Continue with …` buttons | `Google` and `VGU account`, paired and outlined | Three controls in a column starting with the same word is a menu that has to be read before it can be used |
| Privacy · Terms inside the card | At the foot of the screen | Terms are not a control |
| "One more thing" | "Couriers find you by number" | The heading carries the reason; the field label carries the ask. No supporting line needed |
| "Without it nobody can leave you a parcel" in the heading, above a `Later` button | "Parcels need a number." under `Skip for now` | A consequence belongs under the control it is a consequence of, which is where the decision is made — not delivered as a warning before the choice exists |

**Open question, and it is a real one: the number from that last screen is never verified.** The phone path sends a code; this path does not. A typo puts a courier in front of the wrong box, or in front of nobody. Either it needs verification too — which makes the free path cost an SMS after all, and 4 screens against the phone path's 2 — or unverified numbers are accepted knowingly. Not decided here.

**Terminology:** the screens now say *courier*. PRODUCT.md's formal term is *shipper*, glossed there as "a delivery courier". For a receiver reading one line, *courier* is the everyday word; *shipper* usually means the company sending. Worth confirming before it spreads into `strings.xml`.

## The cabinet is the hero, and it is real

The image is the cabinet from `scripts/cabinet-sim/build_cabinet.py`, built from the workshop drawing. Rendered by `scripts/cabinet-sim/hero.py` into `scripts/cabinet-sim/hero/`.

| Choice | Why |
| --- | --- |
| **Three-quarter view, 26°, 105mm** | Chosen by Ryan over a flat elevation. Straight on, the cabinet is a chart of rectangles |
| **Door corners exported with the image** | `hero.py` projects each door's four corners through the render camera into `cabinet.json`. The app draws the highlight as a polygon over the render, so it lands in correct perspective — including door 10, the short letterbox |
| **One render, every state** | Marking a different door, or six free ones, costs nothing |
| **Transparent background, real shadow** | The contact shadow is a shadow catcher under a transparent film, so it renders as alpha. The app gets a cut-out cabinet with a real shadow, on whichever ground it likes |
| **100mm door numbers** | Raised from 72mm *in the drawing*. At 72 the number was hard to read from a metre away at the real cabinet, and illegible in an app that draws it 300px wide. The real cabinet benefits too |

Three things make it read as metal rather than as a CG box, all found by measuring rendered pixels:

- **Something to reflect.** The doors are metallic 0.7, and metal renders by reflecting its surroundings. In a flat black world there is nothing to reflect and it falls back to matte paint. A strip softbox, seen only in reflections, and a gradient sky.
- **Broken edges.** A 1.8mm bevel on every panel. Nothing real has a perfectly sharp 90° edge, and the bright line a fold catches is most of the difference between a photograph and a computed picture.
- **A raking key.** Square-on light makes every door identical and the grid flattens into a chart.

## Liquid Glass

**Ported from [rdev/liquid-glass-react](https://github.com/rdev/liquid-glass-react)** (MIT), whose shader comes from shuding/liquid-glass. Written inline in `liquid.js` because nothing can be bundled at run time.

Three earlier attempts failed, and the failures are the specification:

| Attempt | Why it was wrong |
| --- | --- |
| Blur + white tint + a 1px line | A *painting* of glass. Nothing behind it ever moves |
| Snell's law, n = 1.5, per-pixel ray deviation | Physically correct — and it produces about 9px of bend, which is what 11mm of plate glass really does. Apple is not simulating a sheet of glass |
| Gradient maps at 150px | Bent hard enough, and bent the *whole panel*. Reads as a wobbly rectangle |

What makes it right:

- **The centre is never displaced.** An edge mask is cut from the displacement map's own luminance; the aberration runs only inside it, and an *undisplaced* copy of the backdrop is composited into the middle. Measured: 0.00px at the centre, up to 111px at the rim, zero by 15% in.
- **The map is a shader.** A rounded-rect signed distance field through two smoothsteps, per pixel. The blue channel carries Y — not zero — because the edge mask is cut from the map's luminance.
- **The warp is a separate layer.** `filter:` on a span behind the content, not `backdrop-filter` on the element, so text stays sharp while the backdrop moves.
- **One ring**, a 1px specular arc that goes to zero at both ends, leaning toward the pointer. Apple's edge is a lit arc, not a drawn border; two mask-composited rings at 1.5px with hard inset rings read as chalk on a dark ground.

**The backdrop chain is `saturate() brightness() blur()`, in that order, and the order is load-bearing.** It was `brightness()` first, on the argument that saturation should work on lifted values rather than crushed ones. That is true going down and false going up: brightness above 1 pushes channels past 255, they clamp, and **a clamped pixel has no chroma left for `saturate` to find**.

**The lift is per scheme.** One value, 1.55, was used for both. It was chosen for dark, where a near-black backdrop leaves a pane invisible. In light it took the `#D3DBE3` ground to 327 and clipped every channel: the nav measured `rgb(241,243,245)` — a white lozenge with the card edges behind it erased. Now `1.5` dark, `1.02` light, and a light pane is *tinted* white by a translucent fill rather than gained.

Measured after: nav `rgb(233,238,244)` at chroma **11**, against the card behind it at chroma **6** — the pane carries more colour than what it lies on, at the same luminance. That is the behaviour; a pane flatter than its backdrop is not glass.

Firefox gets the blur alone.

## Colour

Dark leads. Light is a second design, complete, and follows the system setting. A theme toggle is in Settings and in the app bar.

**Why dark leads:** the drafts are dark, glass over a near-black ground carries the depth, and the cabinet render is bright steel with the most to push against on a dark ground.

### Dark — the default

| Token | Hex | Where |
| --- | --- | --- |
| `Ground` | `#090B0E` | the app background, blue-biased, never neutral |
| `Surface` | `#161D27` → `#10161E` | a card, top to bottom |
| `Lip` | `#B0D0FF` 20% | the lit top edge of a card |
| `Hair` | `#96BAE8` 13% | a card's border, and every rule |
| `Field` | `#02050A` 55% | an input, recessed into whatever holds it |
| `Ink` | `#E8EDF4` | text |
| `InkSoft` | `#8E9AA8` | second-rank text |
| `InkFaint` | `#5C6874` | captions and disabled |

### Light

`Ground #D3DBE3` · `Surface #FFFFFF → #F3F7FB` · `Field #E7EDF4` · `Ink #0F161B` · `InkSoft #4C5862` · `InkFaint #7A8792`

The ground is a real mid-steel, not a near-white. At `#F2F5F7` a card had nothing to lift off.

### Every surface is picked, never mixed

A raised surface used to be computed — `color-mix(Ground 62%, Ink)`. Two faults, both visible on screen, both measurable:

| | Ground `#090B0E` | Mixed `rgb(94,97,101)` | Picked `#161D27` |
| --- | --- | --- | --- |
| Blue over red | +5 | +7 | +17 |
| …as % of its brightest channel | **36%** | **7%** | **44%** |
| L\* above the ground | — | **+38** | **+7.6** |

- **A mix goes neutral.** Tint reads against the value it sits on, and pouring a near-white into a colour dilutes that colour by exactly the amount it lifts it. Four fifths of the bias is gone. **The paler the panel, the greyer it reads** — which is the grey panel, arrived at by arithmetic.
- **A mix lifts too far.** A raised panel sits 4 to 8 L\* above what it lies on and takes the rest of its height from a lit edge and a shadow. 38 L\* is not a raised panel, it is a second background.

The picked value is lifted *and more saturated for being lifted*, which is what a real material does when a light finds it.

**A card is lifted by its edge first, its shadow second, its fill last.**

Light breaks the 4-to-8 rule deliberately — white on `#D3DBE3` is +13 L\*. A card cannot go higher than white, so on that side the separation is made by pulling the **ground** down to mid-steel. Same decision from the other end, and the reason one formula can never serve both schemes.

### The tokens animate, the elements do not

Every colour token is registered with `@property`, so a theme change interpolates **one value per token** and everything reading that token follows. What this replaced was a `.theming *` rule putting a seven-property transition on every element on the page — roughly fourteen thousand animations for one tap, which is what made the change stutter, and what made the toggle's own knob refuse to move, since the blanket had to be cancelled on it by hand.

### The accent

**Dark leads blue. Light leads terracotta.** `#7FA8FF → #3D74F0` dark; `#A8482A → #93401F` light. Every control.

The light side changed on 2026-08-08 at Ryan's call — Anthropic's warmth rather than a second blue. It is the one token whose *hue* differs between schemes, which is unusual and deliberate: the cool steel ground of the light scheme has nothing to push against from another blue, and a warm control on cold metal is the whole reason the light design exists.

Picked against the surfaces, not by eye:

| | contrast | verdict |
| --- | --- | --- |
| `#A8482A` on the ground `#D3DBE3` | 4.14:1 | large type and controls |
| `#A8482A` on a white card | 5.79:1 | small type passes |
| white on `#93401F` | 7.4:1 | the filled button |

**It is red-brown, not yellow, and that is the constraint.** The door light is `#FFB200`, and the rule this palette will not break is that amber means *this door is yours* and nothing else. A terracotta control and a yellow door light cannot be mistaken for each other; a golden control and a golden door could. Anything that pulls the light accent toward yellow breaks the one colour the product actually needs to teach.

### The door light

```
DoorLight   #FFB200
OnDoorLight #241A00
```

**It means *this door is yours*.** Two places only: the highlight on the cabinet tab, and the Opened screen, which it fills.

**Decision, 2026-08-07:** the earlier rule was amber *only* on a door that had opened. It now also marks your door on the map, because *amber is my door* is a stronger thing to learn than *amber is open*, and the two moments are the same door four seconds apart. **To reverse it, draw the cabinet-tab highlight in the accent instead** — one argument to `lightDoor()`.

**Identical in both schemes.** It was briefly made theme-dependent — azure on light — on a misreading of which colour was being asked about, and reverted the same day. A door holding your parcel is a fact about the world, not about the phone's settings, and a colour that changes with the scheme is a colour nobody can learn.

**And it is never used as decoration.** A soft amber wash sat in the bottom-left corner of every screen, under the nav bar, as a second light source. Removed 2026-08-07: it read as a yellow stain on a cold interface, and spraying a signal colour across a corner is how it stops being a signal. The screens keep one wash, in the accent, from above the app bar.

**No Dynamic Colour.** Material You would let the wallpaper repaint the one colour that must never be repainted.

### A free box

`#3FD69A` dark, `#0E7A54` light. Free doors on the cabinet map, nowhere else. It never shares a screen with the amber — the receiver either has a parcel here or does not. **One colour at a time** is the rule that protects the amber, and it survives.

### Refusal

`#FF9B92` dark, `#B3261E` light. Semantic, separate from the accent.

## Type

**Geist and Geist Mono**, inlined. Geist for everything; Geist Mono for numbers — box numbers, phone numbers, times, codes — because they line up and change in place.

| Role | Size | Weight | Tracking | Carries |
| --- | --- | --- | --- | --- |
| display | 44 | 650 | −0.045em | the sign-in headline |
| number | 104 | 700 | −0.06em | a box number on the Opened screen |
| headline | 25 | 600 | −0.032em | a screen's one heading |
| body | 14 | 400 | — | reading text |
| label | 12.5 | 550 | — | a card's second line |
| caption | 10 | 600 | +0.15em | a section label, uppercase |

Small type is never lighter than 550. `sp` on Android.

## Words

**People do not read screens, they glance at them.**

| Rule | |
| --- | --- |
| One heading, at most one supporting line | |
| A supporting line is **six words or fewer** | |
| Never explain a control that explains itself | The scan aperture is animated and shaped like a viewfinder. It said *"Point at the cabinet screen / The QR is on the cabinet, not on your phone"* — thirteen words describing an animation. Now: **"Scan the cabinet."** |
| Controls name their action | *Continue*, *Open box*, *Done* |
| Empty is its own screen | An empty list reads as broken |

Budgets held: sign-in **4**, scan **3**, opened **5**, refusal **4**.

Strings live in `res/values/strings.xml`, never in Kotlin.

**Refusal wording is fixed by [api-contract.md](docs/reference/api-contract.md)** and shared with the cabinet. Shortened there on 2026-08-07, before the contract was sent.

## Motion

One authored moment, and it is the point of the app.

| At | What |
| --- | --- |
| 0.00s | the cabinet arrives from 94% and settles — `power3.out` |
| 0.45s | your door lights, and its number is redrawn on top of the amber |
| 1.50s | the screen pushes into that door, 2.9× — `power2.inOut` |
| 2.28s | at rest, looking at your box |

Nothing linear. Same curves as the loader film.

**The number is redrawn because the amber covers the one stencilled on the door.** Placed from the projected corners, not a fixed offset, so it lands correctly on the letterbox door too.

**It plays only when a parcel is waiting.** With nothing waiting there is no motion: free doors are marked, and the screen is a thing to count.

### The theme change

Both halves are Ryan's supplied components, in `docs/designs/wireframe/mobile/components/toggle/darkmode-toggle/`.

**The switch** is ported whole, including the blue sky and the clouds that an earlier pass dropped on taste grounds. Its motion runs on **two** curves, and the gap between them is the entire feel of it:

```
--transition:        .5s cubic-bezier(0, -0.02, 0.4, 1.25)   sky, clouds, stars, moon
--circle-transition: .3s cubic-bezier(0, -0.02, 0.35, 1.17)  the knob
```

The knob crosses in 300ms and the world catches up over 500ms behind it. Flattened onto one curve — which is what a paraphrase of the component did — the knob drifts across in convoy with the background, and that is the soft, laggy motion. Both curves have a negative `y1`, so each dips back before it goes: that is the wind-up, and it is why the knob has weight. Only `--toggle-size` is changed, 30px → 10px, which the component's own `em` sizing scales cleanly.

**The wipe** is one pass, upward. The component ships two — fall 550, swap, rise 550 — with `transformOrigin: "top"` in both phases, so it drops from the top, retracts back to the top, and nothing happens at all for the first 550ms. `duration` is a documented prop, so setting it is sanctioned. Here: the curtain is filled with the **outgoing** colour and held, the theme swaps underneath it, and it then shrinks toward its own top edge — so its bottom edge travels up and reveals the new ground from the floor. **480ms, against 1100.**

**And the blocks turn with it.** Each block gets a delay from how far it sits above the floor of its screen: a 300ms spread, 190ms each, `linear` — the stagger is the motion, and easing every block as well makes the wave mushy. Measured: bottom card moves at 128ms and lands by 307; top card starts at 380 and lands at 579.

Two earlier versions and what each got wrong: **`.theming *`** put a seven-property transition on every element on the page, about fourteen thousand animations for one tap, and had to be cancelled on the toggle by hand — which is why the knob never moved. **Interpolating the tokens on `:root`** cost twenty animations instead, but changes everything at once, uniformly; nothing travels.

**One rule falls out of this, and it is general: anything that varies with the theme goes in `background-color`; the shape of the shading goes in a `background-image` of fixed white and black alphas.** Chrome does not interpolate `background-image` between two gradients — `getAnimations()` on a card returned six running transitions with `background-image` absent, so borders, shadow and text animated while the fill jumped.

Elsewhere: the app-init screen is the `ThinkingOrb` canvas particle system in its `ring` / breathing state.

## What this system will not do

- **No card grid, no icon-plus-heading tiles, no hero metric.**
- **No emoji or unicode glyph standing in for an icon.** Drawn, or not there.
- **No gradient text, and no glass as decoration.**
- **No fourth saturated colour** beyond the accent, the amber and the free-box green — and the last two can never share a screen.

## Not yet decided

**Vietnamese-first.** The sign-in headline is Vietnamese with English underneath. That is a bet, not a decision. See PRODUCT.md.

**Which way in is the default.** The phone field has focus and Google sits under a divider. If SMS cost is the deciding factor it should be the other way round.

## What this replaced

**Version 1, 2026-08-06** argued the box number *was* the product: 104sp on the app's only glass panel, light steel ground, amber spent on one screen, seven flat screens. Rejected 2026-08-07 — the panel read as cheap, and a number on a card is not coherent with a product whose job is a physical door.

**Version 2, 2026-08-07** kept the number card as the end of the cabinet transition and shipped a flat orthographic render. Both rejected the same day: the card again, and the flat view as bland.

What survived both: the steel palette's logic, one-colour-at-a-time, glass used once rather than everywhere, and the inverted scan aperture.

## Where it lives

```
src/app/src/main/kotlin/vn/edu/vgu/smartlocker/
├── ui/theme/             Color.kt · Type.kt · Shape.kt · Theme.kt
└── ScreenFrame.kt        the edges every screen shares

scripts/cabinet-sim/
├── hero.py               renders the cabinet and projects the doors
└── hero/                 cabinet.png · cabinet.json
```

The Compose screens are not written against this yet. The reference is the artifact preview, and the Kotlin follows once the direction is signed off.
