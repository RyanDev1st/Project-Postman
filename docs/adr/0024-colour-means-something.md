# ADR 0024 — Colour means something, so the accent stops being a colour

**Date:** 2026-08-30
**Status:** accepted
**Decided by:** Ryan, who asked for the accent to be worked out from colour theory rather than picked

## The problem

The app had a different accent in each scheme: `#7FA8FF` in dark, `#C2410C` in
light. Two faults came out of that, and one of them is a safety fault.

**The product has no colour anybody can picture.** The only colour on screen
changed from periwinkle blue to burnt orange when the sun went down. A premium
product is recognisable with the labels taken off. This one was not
recognisable with the labels on.

**The light accent sat next to the one colour the product must teach.** The
door light is `#FFB200` at hue 42. The light accent was hue 17 — 25 degrees
away. [DESIGN.md](../../DESIGN.md) already knew this and defended it by
darkness alone: *"a burnt-orange control and a yellow door light are never
read as the same colour."* On a phone, at the size the cabinet tab draws them,
they read as one warm family. A tester pass on 2026-08-29 called the tab cheap
and the reason was underneath the styling: two warm colours on one screen,
neither winning, and one of them is supposed to mean **this door is yours**.

Adding to that, the old light accent was **3.70:1** on the ground — large type
and fills only — and needed a third value, `--accent-ink` `#98380A`, to carry
small text at all.

## The decision

**Colour means something. If it is coloured, it is a fact about a box.**

| colour | what it says | where |
| --- | --- | --- |
| amber `#FFB200` | this door is yours | the cabinet render, the map pin, the Opened screen |
| free green | this box is empty | a free door, a "boxes free" count |
| refuse red | this was turned down | a wrong code, a faulty box |

Everything else is steel. The accent is no longer a hue; it is the **end of
the value scale** — near white on the dark ground, near black on the pale one.

| token | dark | light |
| --- | --- | --- |
| `accent` | `#F2F7FF` — 18.3:1 on the ground | `#0A121B` — 13.5:1 on the ground |
| `accentDeep` | `#E7EEFA` | `#14202C` |
| `accentInk` | `#F2F7FF` | `#0A121B` |
| `onAccent` *(new)* | `#0A0E14` — 16.6:1 on the fill | `#F4F8FE` — 15.5:1 on the fill |

## Why this is the answer and not a second blue

A second blue was the obvious move: amber sits at hue 42, so its complement is
222, which is almost exactly the blue the dark scheme already used. It would
have fixed the clash and given one hue in both schemes.

It was rejected because it fixes the smaller problem. The cabinet tab's job is
to say *which door*, and it says it in amber. Every other coloured thing on
that screen is competing with the only colour that carries information. Taking
the colour out of the controls does not weaken them — they are large, filled
and at maximum contrast — and it leaves amber alone on the screen, which is
the strongest thing that could be done for it. The identity that comes out is
steel and amber, which is a cabinet in a corridor, and it is the same identity
in both schemes.

It is also the register Ryan asked for. Monochrome plus one signal colour is
what a Braun radio, a Leica and a Teenage Engineering box all are.

## What this forces, and none of it is optional

1. **`onAccent` is a real token.** `GoButton`, the theme switch knob and the
   nav pill all wrote `Color.White` on `accentDeep`. Near-white on near-white
   is invisible.
2. **Affordance moves from hue to form.** Links get weight and an underline;
   the disabled primary gives up the primary's fill and keeps an outline; the
   focus ring stays a ring. This is a gain rather than a cost: the tester pass
   found *Use a code* reading as a caption because it was tinted and nothing
   else, and a monochrome system cannot make that mistake.
3. **Headline emphasis inverts.** The lead drops to `ink2` and the payoff word
   keeps `ink`. A near-white word among near-white words is not emphasis.
4. **The ground's top wash comes off the accent and onto `lip`.** A wash is
   light, not colour. In the light scheme an accent wash would draw a shadow
   across the top of every screen.
5. **The scan reticle becomes a fixed white.** It is the one piece of chrome
   that sits over a camera feed rather than over the app's own ground, so it
   has no ground to take contrast from.

## What we give up

- **The reversal in [ADR 0003](0003-parcel-locker-product.md) note is gone.**
  DESIGN.md offered "draw the cabinet-tab highlight in the accent instead" as
  the way to undo amber-means-your-door. There is no accent colour to draw it
  in any more. To reverse amber now, a new signal colour has to be chosen.
- **No coloured brand mark.** If the product ever needs one — a play-store
  icon, a poster — it is amber on steel, and that has to be stated somewhere
  rather than assumed.

## How to check it

`docs/reference/design-tokens.md` is not where this lives; the values are in
`ui/theme/Color.kt` with their contrast ratios beside them, and in
`docs/designs/mockup/screens.css`, which must agree. Every ratio above is
computed with WCAG relative luminance and can be recomputed from the two hex
values alone.
