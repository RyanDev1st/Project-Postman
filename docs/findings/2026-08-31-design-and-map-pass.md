Parent: none

# 2026-08-31 — the palette pass, the press, and the map that was a facade

## Status

**Four commits on `design/token-pass`, none pushed.** Working tree clean apart
from Ryan's own edits to `.claude/` and `CabinetLight.kt`, which were untouched
and unstaged throughout.

| Commit | What |
| --- | --- |
| `9639138` | The palette, the de-boxing, spacing. [ADR 0025](../adr/0025-a-four-point-step.md) |
| `cad84d4` | The day/night switch put back on the Dark mode row |
| `a858887` | `ui/Press.kt` — every control answers a finger. The nav plate slides |
| `a7d62ff` | The map follows the phone, and stops claiming what it cannot know |

`:app:testDebugUnitTest` passes. `checksecrets.py` clean.
`config/settings.json` has `server_base_url` blank, as it must be committed.

**Nothing on the board was ticked.** P1.5-10's Notes were extended with what
today proved; its Verify — a PNG-against-mock-up framing comparison — was not
run, so the task stays open. Board totals unchanged.

## Scope

Two things Ryan asked for, in order: make the app look expensive without
changing its design or its accent, and make the map tell the truth.

### The design half

The palette was redone once already and Ryan said it still looked cheap. He
was right, and the reason was not colour: **every settings row was its own
`CardMaterial`** — shadow, fill, 16dp radius — stacked eight deep with a
recessed icon well on each. Three structural directions were drawn up for that
(artifact, *Three Ways Off The Bubbles*) and **he rejected all three**, along
with four accent directions (*Four Accents Against The Door Light*), and said:
keep the current design, just make it premium.

So the remaining lever was execution, and the gap turned out to be response.
The app had **eleven controls on a bare `.clickable`** with Android's stock
ripple, and exactly two things that moved under a finger — a glass bead and the
theme switch. The two smallest controls in the app answered a tap and the
largest one, the button that opens a locker door, did not.

### The map half

Ryan, testing from home: *"showing stale plotting to the cabinet as opposed to
reflecting the real GPS data."* Four faults, all real, listed under Evidence.

## Evidence

### The press

`ui/Press.kt` — down 90ms, up 200ms, scaling to 0.97 (0.94 small, 0.985 for a
card). Asymmetric on purpose: a control that returns as fast as it left reads
as rubber; one that snaps down and settles reads as a key with a spring under
it. No ripple; the squash already answers *did the tap land*.

Photographed, not assumed — `input tap` is over in a frame, so an explicit
`motionevent DOWN`, screencap, `UP`:

```
primary slab, resting vs held   changed x  11..769  y 584..722
  (the slab is             x 298..482  y 628..663 — so the shadow moved with it)
settings row, resting vs held   changed x  32..748  y 564..692
```

The nav plate slides 260ms with the ink crossing on the same curve; it used to
be drawn inside whichever tab was selected, so it teleported. Verified landing
under HOME, CABINET and SETTINGS in turn.

### The map — four faults

1. **A cached fix had no age.** `getLastKnownLocation` was used whatever its
   age, so a fix taken on the campus stayed "here" for days. The map was not
   lying about the route; it was telling the truth about a place he had left.
   Now a two-minute shelf life.
2. **It ran once.** `LaunchedEffect(granted)` fired on first composition and
   never again — a photograph.
3. **The permission was read, not observed.** A bare `checkSelfPermission` in
   composition with no state behind it: grant location on the map, which is
   where the app asks, and `MapCard`'s copy still said no.
4. **`Walk.Baked` was covering a routing failure.** This is the one that most
   deserves Ryan's word *facade*. Baked is the honest answer to *we do not know
   where you are*; used when the router fails it draws the canned campus line
   captioned `7 min walk · 543 m` for somebody in another city, with exactly
   the confidence of a real route. New `Walk.Unrouted` states the straight-line
   distance from the real position and draws no line.

Fault 4 was found only because the emulator's DNS was broken — a fair model of
bad campus wifi, which is the condition a receiver walking to a locker is most
likely to be in.

Driven with `adb emu geo fix` against the live server over TLS, hardware GL,
real OpenFreeMap tiles, real Valhalla:

| Position | Card says | Map draws |
| --- | --- | --- |
| Ho Chi Minh City, 38 km out | `38 km away · too far to walk` | two markers, **no line** |
| 600 m from the gate | `9 min walk · 811 m` | a route down real streets |
| moved 1.2 km | `13 min walk · 1139 m` | redrawn, no restart |

The baked numbers are `7 min · 543 m`; none of the three is that, which is how
we know the router is being reached.

### Efficiency, every figure from `dumpsys location`

- Backgrounded: **zero** live listeners for the package.
- Ours reads `@+10s0ms, minUpdateDistance=20.0`.
- A fix is not a route — the router is called only past `REROUTE_METRES`
  (120 m) or when the walkable boundary is crossed.
- **MapLibre's own blue dot was asking at `@+1s0ms`** — 140 fixes against our 8
  over three minutes, seventeen times the cost of the route, to move a dot on a
  112dp card. Given a matched engine request; now `@+10s0ms BALANCED`.
- Net: **90 seconds stationary in the foreground delivered 0 fixes.** At the
  old rate that window was about 180.

### Stale numbers found in DESIGN.md

Three, all corrected in place. Amber on `#241A00` read **11.5:1** and measures
**9.50:1**. Amber on the light ground read **1.81:1** and measures **1.61:1**.
The dark accent's ratios had been quoted from `ink` rather than from `accent`.
None changes a decision; all had stood unmeasured.

## Next

1. **BUG-029 needs Ryan's decision.** Seven app-bar controls are drawn as
   buttons and wired to `{}` — menu and bell on Home, back and bell on Cabinet,
   back on Settings, one each in `CodeScreen` and `CabinetScreen`. Not fixed
   here because deleting a back arrow from a tab is a navigation decision, and
   Cabinet and Settings are tabs, so theirs may want to go entirely rather than
   be wired up.
2. **The fused provider has never run.** `play-services-location` is wired in
   and compiles, but the `parity` AVD is an AOSP image with no Play Services,
   so **every location figure above came from the `LocationManager` fallback**.
   The fused path needs a real phone or a Google-APIs AVD.
3. **One Blender run closes BUG-027 and BUG-028 together** —
   `blender -b scripts/cabinet-sim/cabinet_sim.blend --python scripts/cabinet-sim/hero.py`.
   Both are fixed at source; `cabinet.png` still carries the lit screen and the
   stemless `1`, and both are visible in today's Cabinet shots.
4. **The nav glass smears colour** over HOME on Settings now that the blue sky
   switch is back — the pane samples what is behind it. Existing glass
   behaviour, not introduced here. Dial the refraction back on that pane, or
   leave it; Ryan's call.
5. **The dark card at +4.3 L\* is deliberately quiet** and an emulator cannot
   judge that. If it stops reading in daylight on a real phone, the answer is
   the shadow, not the fill.
6. **BUG-020 and BUG-003 still need a real phone.** BUG-020 does not reproduce
   on the emulator — Log out measures ~53dp clear of the nav label — and was
   deliberately left open rather than closed on an emulator's say-so.
7. **P1.5-10 needs one human look** at the map card against the mock-up. Its
   walk half is proved to the metre; only the PNG framing comparison is left,
   and the parity harness cannot render a GL MapView.

## Artifacts published this session

Private to Ryan's account; none is evidence on its own.

- *Four Points of Steel* — the palette report, before/after on five screen pairs
- *Three Ways Off The Bubbles* — three structural directions for Settings, all rejected
- *Four Accents Against The Door Light* — four accent directions, all rejected
