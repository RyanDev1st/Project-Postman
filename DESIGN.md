# Design

The phone app's visual system, written from the built app rather than from intention. Every value here is in the code and was seen on a screen.

Product truth is [PRODUCT.md](PRODUCT.md). The screens and their rules are [docs/reference/app-screens.md](docs/reference/app-screens.md). This file is the *how it looks*, and only that.

## The one idea

**This app exists to walk one person to one door.** The box number is the product, so it is set at 104sp and everything else gets out of its way.

It refuses the parcel-tracking dashboard: no timeline, no map, no status chips, no card grid. A receiver opens this app about twice per parcel, for under twenty seconds, one-handed, sometimes walking.

**Powder-coated locker steel, and one door light.** The whole app is quiet cool grey — the colour of the painted metal the receiver is walking toward — so that the single saturated colour means something when it arrives.

Direction pinned by the user on 2026-08-06: depth, liquid glass, modern, a 3D feel, minimal, UX-led. Rendered through Material 3, because the platform is Android and iOS controls here would be a costume.

## Colour

`src/app/.../ui/theme/Color.kt`. Material colour roles, so light, dark and contrast variants resolve on their own. Raw hex in a screen file is a bug.

### Steel — light

| Token | Hex | Where |
| --- | --- | --- |
| `SteelGround` | `#D5DDE4` | the app background |
| `SteelSurface` | `#FFFFFF` | a raised card |
| `SteelSurfaceAlt` | `#E4EAEE` | a recessed field |
| `SteelInk` | `#12181C` | text |
| `SteelInkSoft` | `#4C575F` | second-rank text |
| `SteelOutline` | `#97A3AB` | field borders |
| `SteelPrimary` | `#1E2A32` | the one button that matters |

**The ground is a real mid-steel, not a near-white.** This was found by screenshot: at `#F2F5F7` a white card had nothing to lift off, and the depth the brief asks for did not exist. Raising the ground is what makes the glass panel a slab instead of a rectangle.

`SteelInkSoft` is tinted from the ground's own hue. A neutral grey on a blue-grey ground reads as unconsidered.

### Steel — dark

| Token | Hex |
| --- | --- |
| `NightGround` | `#0D1114` |
| `NightSurface` | `#171E23` |
| `NightSurfaceAlt` | `#212B31` |
| `NightInk` | `#E7EDF1` |
| `NightInkSoft` | `#97A5AE` |
| `NightOutline` | `#46525A` |
| `NightPrimary` | `#C5D5DE` |

Dark is a second scheme, not an inversion, and it was rendered before that was claimed. The glass panel becomes smoked rather than white: a 13%-white body on a near-black ground, with the specular edge dropped to 16% so it suggests an edge instead of drawing one.

**Light is the default, and the scene chose it.** A student glancing at a phone outdoors on a Vietnamese campus, in daylight. The corridor at the cabinet is the exception, and the amber screen is what covers it.

### The door light

```
DoorLight   #FFB200
OnDoorLight #241A00
```

**One colour, one screen, whole screen.** It appears only when a door has opened, and it fills that screen. A person can tell it from every other screen at three metres, in a corridor, without reading a word.

Two rules keep it worth something:

- **It is spent nowhere else.** No amber accents, no amber chips, no amber highlight anywhere in the app.
- **It is identical in dark mode.** A door being open is a fact about the world, not about the phone's settings — and the corridor is dark either way. Verified on screen, not assumed.

**No Dynamic Colour.** Material You would let the phone's wallpaper repaint the app, including the one colour that must never be repainted. A user learns that amber once and then trusts it; handing it to a wallpaper is a real cost for a decorative gain.

### Refusal

`RefusalLight #B3261E`, `RefusalDark #FFB4AB`. Semantic, and separate from the accent.

## Type

`src/app/.../ui/theme/Type.kt`. The Material type scale, with one role pushed hard.

**Roboto stays.** It is the system face, it is Android's own guidance, and it is right for a screen read in two seconds while walking. Character comes from scale and tracking, not from a novelty face the phone must load first.

| Role | Size | Weight | Tracking | Carries |
| --- | --- | --- | --- | --- |
| displayLarge | 104sp | Bold | −0.045em | **the box number, and nothing else** |
| displayMedium | 68sp | Bold | −0.04em | a box number in a list |
| displaySmall | 40sp | SemiBold | −0.02em | — |
| headlineMedium | 27sp | SemiBold | −0.02em | screen titles |
| headlineSmall | 23sp | SemiBold | −0.015em | screen titles |
| titleLarge | 19sp | SemiBold | −0.01em | cabinet name |
| bodyLarge | 16sp | Normal | — | reading text |
| bodyMedium | 14sp | Normal | — | second-rank text |
| labelLarge | 15sp | SemiBold | +0.01em | button text |
| labelMedium | 12sp | Medium | +0.09em | the `BOX` caption |

Two rules:

- **Nothing but the box number uses a display role.** The moment a second thing is that big, neither is.
- **Small type is never lighter than Medium.** It has to survive corridor light, and it is given room by tracking rather than by size.

`sp` throughout, so type follows the system font-size setting.

## Shape

`src/app/.../ui/theme/Shape.kt`. Thick corners: 8 / 12 / 18 / 24 / **32**dp.

Glass has depth, and a thin radius reads as a cut edge rather than a rounded one. The parcel card uses `extraLarge`, and that is what makes it a slab lifted off the ground rather than a rectangle drawn on it.

## Depth, and the one glass plane

`src/app/.../ui/Glass.kt`. Two pieces, and they only work together.

**`LockerBackdrop`** — a single soft light falling from the upper left across painted metal, both stops within a few percent of the ground. Nearly invisible alone; it exists so the card above it has something to float over.

**`GlassPanel`** — the app's only glass. It carries the waiting parcel and nothing else. Glass on every container is a texture; glass used once says *this is the thing you came for*.

Three parts make it read as a thick slab. Drop any one and it flattens:

1. A shadow with real offset and blur — 24dp light, 4dp dark, tinted `#0B1620` rather than pure black.
2. A body lighter at the top than the bottom.
3. A 1dp specular hairline along the edge, where a light from above would catch.

**The body is opaque in light mode**, and that is a fix rather than a preference. A translucent body let the elevation shadow show *through* the card and painted a hard-edged plate inside it. Against pale steel a translucent white read as white anyway, so the transparency bought nothing and cost the artifact.

**The aperture is the glass panel inverted.** On the scan screen, a near-black square cut *into* the surface, with four L-shaped corner marks drawn on Canvas. One is lit and lifted toward you; the other is dark and cut away. One says *here is your parcel*, the other says *look through here*.

## Layout

- **Everything tappable sits in the bottom third.** One hand, standing, possibly holding a parcel.
- **The primary button is full width and at least 58dp tall** — above Material's 48dp floor, because this one is pressed while walking.
- **The parcel card sits high**, where the eye lands. Centring it left a dead top third and pushed the number away from the thumb.
- **`ScreenFrame` owns the edges**: safe-drawing insets, 24dp horizontal, 20dp vertical. No screen re-implements them.
- Edge to edge, with the system bar icons following the ground behind them.

**The Opened screen sets its own bars.** It has no scheme — it is always amber — so in dark mode the phone drew white icons on bright yellow and the clock became hard to read. It forces dark bar icons while it is up and restores the theme's on the way out. Found by screenshot in dark mode; it does not appear in light.

## Copy

Strings live in `res/values/strings.xml`, never in Kotlin, so wording can be argued over without touching code and a Vietnamese translation is a second file rather than a rewrite.

- **Controls name their action.** *Scan to open*, *Open my box*, *Send me a code*.
- **Refusal wording is fixed by [api-contract.md](docs/reference/api-contract.md)** and is never invented per screen.
- **Empty is its own screen**, not an empty list. An empty list reads as broken.
- The back door — *Can't scan? Use a code* — is quiet text, never a second big button.

## What this system will not do

- **No second accent.** One saturated colour, one screen. Anything else devalues the amber.
- **No card grid, no icon-plus-heading tiles, no hero metric.** The parcel is the content.
- **No stock container inside a committed screen.** A Material `OutlinedCard` was used for the viewfinder and replaced; it read as a component, not as an aperture.
- **No emoji or unicode glyph standing in for an icon.** Drawn, or not there.
- **No gradient text, and no glass as decoration.** The blur has one job.

## Not yet decided

**Motion.** There is none. The app has no authored moment yet, and one belongs on the transition into the Opened screen — the door light arriving is the emotional beat of the whole product. It waits until that screen is driven by a real server answer rather than a tap.

**Vietnamese-first.** Both languages are on screen; which one leads is open, and it matters more on the cabinet than in the app. See PRODUCT.md.

## Where it lives

```
src/app/src/main/kotlin/vn/edu/vgu/smartlocker/
├── ui/
│   ├── Glass.kt          the backdrop and the one glass panel
│   └── theme/
│       ├── Color.kt      steel, night, and the door light
│       ├── Type.kt       the Material scale, display pushed hard
│       ├── Shape.kt      thick corners
│       └── Theme.kt      the schemes, and the direction contract
└── ScreenFrame.kt        the edges every screen shares
```

The direction contract is the comment at the top of `Theme.kt`. It states what this surface owns and what it refuses, and it is what the next person should read before changing any of the above.
