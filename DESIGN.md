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
| **Home** | State as the headline, greeting demoted above it. A card per waiting parcel, each led by a **crop of the real cabinet with its door lit**, and carrying how long is left as a bar. Then **Collected** underneath |
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

**Dark leads blue. Light leads burnt orange.** Changed 2026-08-08 at Ryan's call — warmth rather than a second blue. It is the one token whose *hue* differs between schemes, which is deliberate: the cool steel ground of the light design has nothing to push against from another blue, and a warm control on cold metal is the reason that scheme exists at all.

| Token | Dark | Light | Carries |
| --- | --- | --- | --- |
| `--accent` | `#7FA8FF` | `#C2410C` | the colour, wherever it is large or sits on a card |
| `--accent-deep` | `#3D74F0` | `#B83E0B` | the filled button |
| `--accent-ink` | `#7FA8FF` | `#98380A` | small accent text on the ground |

**Three values, not one, because orange is lighter than a red at the same chroma** and loses contrast against a mid-steel ground. No orange worth calling orange clears 4.5:1 on `#D3DBE3`. Measured:

| | contrast | |
| --- | --- | --- |
| `#C2410C` on the ground `#D3DBE3` | 3.70:1 | large type and fills only |
| `#C2410C` on a white card | 5.18:1 | small type passes |
| `#98380A` on the ground | 5.17:1 | small type passes here too |
| white on `#B83E0B` | 5.63:1 | the button |

`--accent-ink` exists for one element: the 10px `See all` link, the only accent text that sits on the ground rather than on a card. `--accent-deep` will not serve — in dark it is `#3D74F0`, which is 4.26:1 on `#090B0E`, so using it there would fix light by breaking dark.

**The button fill is deliberately not pushed darker.** A first pass filled it with `#98380A`; at that value the largest orange area on the screen goes brown, and the button is what decides whether the scheme reads as orange at all. A prior pass at `#A8482A` was rejected the same day for the same reason — red-brown, not orange.

**And it stays off yellow, which is the constraint rather than the taste.** The door light is `#FFB200` at hue 42°; this sits at hue 17°, 25° away and much darker. Amber means *this door is yours* and nothing else. A burnt-orange control and a yellow door light are never read as the same colour; a golden control beside a golden door would be. Anything that pulls this accent toward yellow closes that gap and breaks the one colour the product has to teach.

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

## Home carries information now

Home used to lead with **Chào Minh** at 25px and put the only fact on the screen — how many parcels are waiting — underneath it in 13px grey. A greeting is warmth, not news, and nobody opens this app to be greeted. The two swapped.

Three things were missing, and they are why it read as bland: **bland and uninformative were the same fault.**

| Was | Now | Why |
| --- | --- | --- |
| Two parcel cards, visually identical | Each carries **how long is left**, as a label and a bar | One parcel may be six hours from expiring and the other thirty-one. Identical cards say those are the same thing. The typed code lives 48 hours — that is real product truth from PRODUCT.md and it was nowhere on the screen |
| A tinted chip with the box number | A **crop of the actual cabinet, that door lit** | `04` tells you what to look for. A picture of the wall of doors with yours lit tells you *where to look*, which is the question you are standing there asking. Placed from the same projected corners the cabinet tab uses, off a clone of the one `<img>` — no extra bytes, and it cannot drift out of alignment with the render |
| Empty Home: a dashed box, *"We'll tell you when one arrives"* | **6 boxes free**, with those doors marked green on a cabinet crop | A placeholder admitting there is nothing to show. But there is: whether this cabinet has room, so someone can be sent to it today |

The amber on a Home card means what it always means — *this door is yours*. A parcel not yet collectable is marked in the accent instead, never the amber, and free doors in the free-box green. One colour at a time survives.

## Home, relaid out: one door owns the screen

Adding facts to the cards did not stop Home being boring, because the fault was never the content. It was the **shape**: a heading, two parcel cards and three history cards — **five rounded rectangles of near-identical height, stacked, on one gap.** Every fact on the screen was set in the same box at the same weight, so the eye had nowhere to land and nothing to do but read top to bottom. That is what bland means structurally, and no amount of material fixes it, because what the layout was saying is that nothing on it matters more than anything else.

The product does make that claim, and it was already decided: with a parcel waiting, the receiver sees **one parcel**. So Home now has three ranks, and nothing on it is the same size, shape or material as anything else.

| Rank | What | Material |
| --- | --- | --- |
| 1 | **The arrival.** A picture of the actual door running edge to edge, the door number at 27px, the time left, a full-width meter, and the button | None — the image bleeds and the type stands on the ground under it. Not a card |
| 2 | **Also waiting.** One row, tappable, clearly second | `.recess` — set into the ground |
| 3 | **Collected.** A hairline ledger, no fill, no border, no radius | None — a rule between entries |

Consequences worth stating:

- **The greeting moved into the app bar.** It was an 11px uppercase eyebrow over the headline, and an eyebrow is a label apologising for a heading that should carry itself. The name still belongs on Home — it is how you confirm this is your account — and an app bar is where that context lives. `VGU Locker` was not earning the slot; you know which app you opened.
- **`Open` stopped being a text link.** The primary action of the whole product was 12.5px accent text inside a card, with a 25px tap target. It is now a full-width filled button that names the door it opens.
- **The empty Home is the same screen.** Same band, same lockup, same ledger — different news. It was a second design; now it is one screen with a count instead of a countdown, and the ledger runs longer because there is room for it.
- **Amber stopped being ink.** `--door` `#FFB200` measures **1.81:1 on white** — the *6h left* label was legible in dark and effectively invisible in light. Tinting it darker would fix contrast and break the palette: the nearest legible amber is hue 28 and the light accent is hue 17, so the two would read as one colour. So amber became a **surface** — `#FFB200` on `#241A00`, the pairing the Opened screen already fills a whole screen with, at 11.5:1, identical in both schemes.

### The band always covers

A band that bleeds to both screen edges has a requirement the 62px square thumbnails never had: **no corner of it may be empty.** Metal stopping two thirds of the way across does not read as *the cabinet ends here*, it reads as a broken image.

Two faults had to be fixed to get there, and the first was a real bug:

- **`paintDoorshot` scaled width and height independently.** `imgW = w * scale, imgH = h * scale` is only aspect-correct while `w === h`, and it was, for as long as every crop was square. On a 306×138 band it stretched the render to 2.2:1 — doors twice as wide as the real thing, in a picture whose entire job is to show you what to look for. It is one square side now, fitted; a square box gets exactly what it got before.
- **Per-band `fill` constants cannot work.** `fill` sizes the *marked* doors, and those are a different shape every time — one door on Home, six scattered ones on the empty Home — so the number that fills one band leaves a hole in the other, and both drift the moment a band's height changes. `fit: "cover"` sizes from the full run of doors instead and clamps the centre so the metal never pulls away from an edge. The same rule `background-size: cover` follows, and the same clamp the cabinet tab's push-in uses.

The empty band is the one deliberate exception: it does **not** cover. Cropping it showed three of the six free doors plus the control panel — the most saturated thing on the cabinet and the one part that is not a door — which undercuts the only fact that screen exists to state. The whole cabinet stands in a taller band with all six marks visible, as an object on the ground. Same reading as the sign-in hero, and it can afford the height because that screen has no button.

## Three materials, three jobs

Glass was the only material this app had, and glass floats. Everything that was not floating chrome had to be either a card or nothing, which is why a field, a number chip and an icon well all looked like the same flat grey square with a hairline round it.

| Material | Gesture | Where |
| --- | --- | --- |
| `.lg` | Floats **over** content | the nav bar, the app-bar buttons |
| `.card` | A plane lifted **off** the ground | grouped rows, the profile |
| `.recess` | Set **into** the ground | fields, code boxes, number chips, icon wells, the second parcel |

A recess costs two shadows and nothing else: the ground's own shade thrown inward from the top edge, and a lit return along the bottom where the surface comes back up. That is the honest version of soft UI — no outer shadow, no fake extrusion, no pair of grey blurs on both sides of a floating lozenge. Both shadows are tokens, so a theme change interpolates them with everything else, and the light scheme **swaps their roles** rather than reusing the dark numbers at a lower alpha.

**A recess must be darker than what it is cut into, and in light mode it was not.** Measured: ground `rgb(211,219,227)`, `--field` `rgb(231,237,244)` — twenty points *up*. Every recessed thing in the light scheme was reading as a raised white chip with two shadows painted on it, which is exactly the mistake the material was written to avoid. `--field` is now `#C6D0DA`, thirteen points down, and still carries `--ink-2` at 4.67:1.

### Defects found and fixed in the same pass

| Defect | Fix |
| --- | --- |
| The Settings avatar had **no CSS rule at all** — `.parcel .num` was deleted when the parcel cards became door crops, and the initials had been unstyled text on the card ever since | A real `.me .avatar`, accent-tinted with an inset ring |
| The nav's selected pill was a 34% accent wash under a 55% accent shadow spreading 8px — colour under colour is a glow, and the glow is what made it look cheap | Fill down to 24%, the coloured shadow replaced by a neutral offset one, the lit top edge up. Edge first, shadow second, fill last — the same order as `.card`. The `Open` button's own accent glow went for the same reason |
| Three different nav icon sets across three screens, and the replacement gear read as a **sun** at 17px | One sliders icon everywhere |
| `aria-selected` on plain buttons; toggle switches were `<div>`s with no name or state | `role="tablist"`/`role="tab"`, and `<button role="switch" aria-checked>` |
| The OTP and add-phone screens centred their heading, leaving a 180px hole between the question and the answer | `.authmid` sits the heading just above the form |
| Two app bars used a `visibility:hidden` spacer instead of the `.side` structure the others use | `.side` |
| Section spacing lived as three different inline `padding-top`s (20, 22, 18) and no rule at all on the other two | One `.slabel` rule, more space above than below |
| Dead CSS: `.field*`, `.badge`, `.statuspill` + its keyframes, `.tokens`/`.swatch`/`.scale`, `.row*`, `.parcel*`, `.freeboard`, `.hello`, and a `.dot-btn` background that `.lg` had always overridden | Removed |
| The Loading caption still described the orb's `breathing` ring state after the renderer was changed to `globe` | Corrected |

## Motion

**GSAP 3.15 core, inlined at build time.** The artifact CSP blocks every runtime request, so nothing is fetched; the library is embedded as a script by `build.py`, which costs 71KB on a 1.8MB page.

### The philosophy, and it is short

A receiver opens this app about twice per parcel, for under twenty seconds, one-handed, often walking. That is the whole brief, and three rules fall out of it.

**1. Motion has to answer a question faster than stillness would** — not decorate the answer, *be* the answer. The cabinet sequence exists because *which of those doors is mine* is hard to say in words and trivial to say by lighting one and pushing into it. The expiry bars **drain** rather than appear, because the useful thing is not the number of hours but the sense that one parcel is nearly out of time and the other is not. Anything that fails this test is deleted, not shortened.

**2. One authored moment per screen. Everything else is feedback.** Feedback confirms a tap and leaves — under 200ms, no overlap, no sequence. The authored moment gets a second and a half, and there is exactly one.

**3. Nothing bounces.** `back` and `elastic` are banned, and not on taste grounds: this interface opens a locker holding someone's property, and an overshoot reads as *approximate*. Metal that springs is metal that is not latched. The single exception is the theme toggle, which is a supplied component carrying its own overshoot — a switch is allowed to feel sprung.

`power3.out` throughout, `power2.inOut` for anything that travels far. Both are already the loader film's curves, so the app and the film move the same way.

### Why GSAP and not CSS

The cabinet sequence was four `setTimeout`s driving CSS transitions. That works until something has to be interrupted, reversed, slowed for inspection, or kept in step — and all four were needed. A timeline is one object with a playhead: `.timeScale(0.25)` is the slow-motion control for free, and `.kill()` on a mode switch is a guarantee rather than four `clearTimeout`s and a hope. It also removes a bug class this build has paid for twice — a transition that silently never runs because its "before" style was never established in its own frame. GSAP records the start value itself.

`gsap.matchMedia()` handles `prefers-reduced-motion`, and reduced motion is **not the same animation slowed down**: nothing travels, nothing fades, and the bars are simply drawn at their real value — which is the information the motion existed to carry.

### The one authored moment

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

### The loading orb is a globe, not a ring

Three attempts. The first was three blurred blobs — a lava lamp, not the component. The second read the component's parameters correctly and then invented the geometry: flat concentric circles with a faked depth value. Right numbers, wrong shape, which is the worst kind of wrong because the numbers make it look researched.

The third transcribed the real renderer — and revealed that the literal answer was also wrong. The component maps `breathing → "ring"`, and `breathing` is labelled *"Thinking…"*. But `ring` renders a thin hoop of dashes, and that is not a porting bug: when `faceOn` is set the basis is built at pitch `−c` and the projector at `+c`, so the two cancel and every band collapses onto `(cos θ, sin θ) · r / √(1+I²)`. Eleven bands at 0.075 spacing span only `0.936r` to `r`. **It is called `ring` because it is one.**

The spheres are `globe` — latitude rings whose dot count follows `|cos(latitude)|`, so they crowd at the equator and converge at the poles, spun on a yaw with a highlight sweeping round. That is what this screen uses, and it is the better fit anyway: the caption is *Checking your parcels*, which is a search, not a thought.

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
