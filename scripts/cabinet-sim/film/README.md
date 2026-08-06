# Film

Videos built from the cabinet simulator. Two of them, for two jobs.

**This is not evidence.** Same line as [ADR 0008](../../../docs/adr/0008-cabinet-simulator.md): a rendered locker proves nothing about a real one. These exist to explain the product to people, and to fill the app's first second. Never to tick a task whose Verify names the real cabinet.

| File | What it makes | State |
| --- | --- | --- |
| `hand.py` | The wood material. Kept because the material is shared | done |
| `rig_person.py` | Cuts the mannequin into rigid pieces and rigs its right arm | done |
| `shot_loader.py` | **The short one.** A parcel goes into box 04 and the door shuts | **done — 4.5 s** |
| `story.md` | Direction for the long film: nine beats and the subtitles | done |
| `scene_gate.py` | The campus gate the long film is shot at | **unfinished — see below** |

Renders land in `out/`, which git ignores.

## The long film is not finished

The direction is written (`story.md`) and the set is built (`scene_gate.py`), but **no shot of it has been rendered and it is not cut.** Do not treat it as ready.

Where it got to, and what is actually wrong with it:

- **The set stands up.** Wall, piers, canopy, road, planting, and a sun that lights the cabinet face. The canopy casts a readable shadow.
- **The sky is wrong.** `citrus_orchard_road_puresky` is a *sunset* HDRI, and it is dim. Turning it down far enough to stop it flattening the shadows leaves the sky darker than the sunlit ground — which reads as a coming storm, not as afternoon. **Fix: pick a midday or morning-afternoon HDRI**, not a sunset one, and re-check the numbers below.
- **Exposure is set by measurement, not by eye.** Sample the render and aim for sunlit concrete near 0.5 and the wall near 0.65. There is a working method in the git history for this: render to a file, load it back, and read the pixels.

Three faults were found and fixed the slow way, and they are worth knowing about before touching this again:

1. **`_mat()` used to return early** if a material already existed, so `build_gate()` rebuilt every object and kept the old materials. Editing the palette changed nothing on screen, which reads as "the fix did not work" rather than "the fix was not applied".
2. **The sun pointed the wrong way.** At `(56°, 0, −118°)` the light travelled from behind the wall, so the cabinet face and the whole forecourt were backlit. Every attempt to fix the flatness by raising the energy made a brighter flat picture. It was never an exposure problem.
3. **Albedos were painted, not measured.** Concrete at 0.70 and wall at 0.76 give a white-out under any exposure. Real concrete is about a third.

## Running it

Needs the cabinet built first — `build_cabinet.py`. Then, in Blender's Scripting tab or through the MCP addon:

```python
base = r'scripts/cabinet-sim/film'
for f in ('hand.py', 'rig_person.py', 'shot_loader.py'):
    exec(open(f'{base}/{f}').read())
rig()          # cut and rig the figure. Safe to run again
setup()        # build the set, frame it, animate it
render()       # straight to out/loader.mp4
```

`setup()` and `render()` are separate on purpose. Checking the framing in the viewport costs a second; checking it in 135 finished frames costs a minute.

## The short one — the app loader

Plays while the app is starting. 720 × 1280, no words, no sound, about 630 kB.

Three things it has to get right:

1. **It ends on `#CFCBC2`** — the app's own background — so the interface draws over the last frame with no visible step. That colour is in `shot_loader.py` as `HOUSING`. If DESIGN.md moves it, move it here in the same change.
2. **Box 04**, because the app says 04 everywhere else.
3. **Straight in, not across.** The hand travels along one axis. An arc read as reaching around something, and swung the parcel through the open door's edge.

## Why the figure is a wooden mannequin

It is the model the scene already had, and it is the right one. No face, no fingers, no skin — so nothing in frame is pretending to be a real person, and there is no uncanny valley to fall into. It also sits with the German-industrial language the app uses: a drawing-room teaching model.

**The whole figure, never a floating hand.** A disembodied hand reads as a ghost.

## What `rig_person.py` had to do

Hunyuan reconstructs from photographs, so wherever two surfaces touch it welds them. This figure's arm is fused to its hip down the length of the forearm. Lifting the arm dragged a sheet of hip with it — on screen, a wooden rod from the shoulder to the floor.

No weighting scheme fixes that; the geometry really is joined. So the mesh is **cut** at the joints, each opening is capped, and a wooden ball is dropped over every cut.

Which is also what the object is. A real artist's mannequin is separate turned pieces on ball joints, and nothing about it bends. Rigid segments are not a shortcut here — they are the thing being modelled.

The cut runs once and sets a flag on the mesh. Running it twice would cut the caps off the first cut and grow the mesh every time.
