# ADR 0025 — A card sits four points above its ground, in both schemes

**Date:** 2026-08-31
**Status:** accepted
**Decided by:** Ryan, who looked at the repainted screens and said the colour
mixes looked bad, and asked for something that looks expensive

## The problem

[ADR 0024](0024-colour-means-something.md) took the colour out of the accent.
It fixed what it set out to fix and the screens still looked cheap. Ryan said
so directly. The honest account of how that happened: the palette was derived
from one constraint — *the accent must not be a colour* — plus contrast
arithmetic. That is colour **science**. Nobody had looked at what products
that people pay for actually do.

So we measured ours against two published systems, [Radix
Colors](https://www.radix-ui.com/colors) (the Slate scale, which exists to be
a cold grey) and [Vercel Geist](https://vercel.com/geist/colors).

**The step from a ground to a card was a stage, not a step.**

| | ground → card, in L\* |
| --- | --- |
| Radix Slate, light | −1.0 |
| Vercel Geist, light | +2.0 |
| Radix Slate, dark | +2.9 |
| Vercel Geist, dark | +6.3 |
| **Postman, light** | **+14.1** |
| **Postman, dark** | **+7.5** |

Fourteen points came out of a rule this repo wrote down and defended: a card
cannot go higher than pure white, so pull the *ground* down until the card
lifts off it. The arithmetic was right. The result was a mid-steel backdrop
with sheets of paper laid on it — two materials, where a premium screen has
one material with things raised out of it.

**The neutrals were blue, not tinted.** Ours ran **20 to 28 percent**
saturation. Radix Slate, which is the reference for a cold grey, runs 6 to 20.
At 28 percent the ground is a colour, which breaks ADR 0024 before a single
component gets a chance to.

**A card wore five materials at once.** `CardMaterial` stacked a drop shadow,
a fill, a vertical gradient over the fill, a 1dp hairline all the way round,
and a 2dp lit top edge. Each one is defensible alone. Together they are a card
outlined, shaded, lit and lifted — a card shouting that it is a card. Across
the app: 11 borders, 13 gradients, 5 shadows.

## The decision

**One material, and small steps inside it.**

1. **Ground to card is +4.3 L\* in dark and +3.5 in light.** The light ground
   comes *up* to `#F0F2F4` and the card stops short of white at `#FAFBFC`. A
   card that is nearly the value of its ground is told apart by its edge and
   its shadow — which is what DESIGN.md already said and the palette was not
   letting it do.
2. **Every neutral is hue 210 with about two points of channel spread.**
   Nothing is a pure grey; nothing is a pure white. The cold bias is amber's,
   not ours: `#FFB200` is hue 42, so on a cool field it is the warmest thing
   on the screen and it carries. On a warm field it goes muddy, which is what
   happened to the burnt orange ADR 0024 removed.
3. **A card is a shadow and a fill.** Nothing else. The gradient, the hairline
   and the lit top edge are gone.
4. **A highlight catches an edge, never a face.** The primary slab's sheen ran
   its full 52dp at 22% white, lifting the head of a near-black button by
   twenty points of value. It is now 10% over the top eighth.
5. **A hairline has to earn itself.** It stays where it carries meaning — an
   error state, a filled input cell, the disabled primary, an outline button
   that *is* its outline — or where two surfaces genuinely cannot be told
   apart, which after this change is the selected nav plate sitting on glass
   that has already sampled and darkened the ground.

## What this costs

The dark scheme is less dramatic. A `#101214` ground is graphite rather than
the near-black `#090B0E` it was, and a card at `#1A1D20` no longer announces
itself. That is the point, and it is the thing to check on a real phone in
daylight rather than on an emulator: if the card stops reading outdoors, the
answer is the shadow, not the fill.

## What this does not change

ADR 0024 stands entirely. Amber still means *this door is yours*, green still
means *this box is free*, red still means *this was refused*, and the accent
is still the end of the value scale rather than a colour. This ADR is about
the steel between them.

## Evidence

- `src/app/src/main/kotlin/vn/edu/vgu/smartlocker/ui/theme/Color.kt` — the
  token sets, with the measured ratio on each line.
- `docs/designs/mockup/screens.css` — the same values for the cabinet screen.
- Screens shot on the `parity` emulator, Android 16 x86\_64, hardware GL,
  against the live server over TLS, on 2026-08-31: all three tabs in both
  schemes.
