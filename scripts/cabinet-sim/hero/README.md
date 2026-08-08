# The cabinet, as the app draws it

Built by `../hero.py`. Do not edit either file by hand.

| File | What it is |
| --- | --- |
| `cabinet.png` | The cabinet, orthographic front elevation, transparent background with a real shadow. 2000 × 2250 |
| `cabinet-1600.png` | The same, at 1600 × 1800, for shipping in the app |
| `cabinet.json` | Where each door landed, as normalised corners. The app draws its highlight from this |

Regenerate both together whenever the camera, the lighting or the drawing changes:

```bash
blender -b ../cabinet_sim.blend --python ../hero.py
```

They must stay in step. A `cabinet.json` from one camera over a `cabinet.png` from another puts the highlight on the wrong door, and nothing will warn you.
