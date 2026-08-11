# The mock-up, kept as source

The current design of the phone app, exactly as it was when the last design
session ended (2026-08-11). This folder is the **source of truth** for the
design port — DESIGN.md is a record and can lag the screens.

| File | What it is |
| --- | --- |
| `body.html` | every screen's markup, with the reasoning inline |
| `screens.css` | the app's tokens (dark + light) and every component style |
| `screens.js` | the interactive behaviour: lit doors, the panel, the switch |
| `glass.css` | the liquid-glass material the chrome is built from |
| `liquid.js` | the refraction shader the glass runs on, inlined at build |
| `motion.js` | the one authored moment on the cabinet tab, and the drains |
| `orb.js` | the thinking orb on the loading screen |
| `campus-symbol.svg` | the campus plan, projected from OSM by `map.py` |
| `map.py` | fetches the ways and the Valhalla route, bakes the symbol |
| `route.json` | the engine, the date and the maneuvers the symbol came from |
| `build.py` | inlines fonts, GSAP and the render into one artifact page |

The built artifact is `preview.html` (self-contained, ~2 MB); rebuild it with
`python map.py && python build.py` from the folder it was generated in.

## What the port must reproduce

- **Tokens** in `screens.css` lines 46–151 — dark leads, light is a second
  design. Amber `#FFB200` is identical in both schemes and means *this door is
  yours*.
- **Three materials**: glass floats over content (nav, app-bar buttons), a
  card lifts off the ground (rows, profile), a recess is set into it (fields,
  code boxes, number chips).
- **The cabinet tab**: the render from `scripts/cabinet-sim/hero/` with the
  door projections from `cabinet.json`; amber tint keeps the stencilled number
  (`mix-blend-mode: color`); one panel in every state; the frame pushes in on
  its own only when exactly one box is yours.
- **Home is a claim ticket**: big number, seam, time left on the foot, the map
  card answering *where to walk*, the ledger of what you collected.
- **Words**: scan is three words, opened is five, refusal is four.

The Android port lives in `src/app/src/main/kotlin/vn/edu/vgu/smartlocker/`.
