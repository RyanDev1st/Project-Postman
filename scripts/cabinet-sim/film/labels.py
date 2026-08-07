"""Weld each shipping label into the parcel it belongs to.

Hunyuan bakes text into a parcel's texture as unreadable mush, so the label
goes on as a separate decal - `apply_labels.py` draws it and shrinkwraps it
onto the box. That leaves two objects that have to travel together, and twice
now they have not:

1. Unparented, the parcel flew into the locker and the label stayed on the
   floor where it was made.
2. Parented, it was worse and harder to see. `matrix_parent_inverse` was
   recorded from `parcel.matrix_world` at whatever frame happened to be
   current - and the parcel was already mid-animation - so the offset baked in
   was the one from a moved box. The label ended up hanging in mid-air a metre
   from its parcel, and the guard that made the operation idempotent
   (`if label.parent is not None: continue`) meant it was never re-fitted.

So it is not parented any more. **It is joined into the parcel mesh**, as an
extra material slot. A label that is part of the box cannot slide off it,
cannot be left behind, and has no offset to record wrongly. The wrong state
stops being representable rather than being carefully avoided.

    exec(open(r'scripts/cabinet-sim/film/labels.py').read())
    weld_labels()
"""

import bpy
import math

# The transform each parcel was imported at. The label has to be fitted with
# the parcel sitting here, because `apply_labels` measures the top face in
# world space - fit it while the box is inside a locker and the label is cut
# to the shape of wherever it happened to be.
IMPORT_ROT = (math.radians(90), 0.0, 0.0)
IMPORT_SPOT = {
    "BlackWrap":  (-0.75, -1.05, 0.0),
    "BubbleWrap": (-0.41, -1.05, 0.0),
    "PolyMailer": (-0.07, -1.05, 0.0),
    "ShopeeBox":  (0.27, -1.05, 0.0),
    "Tube":       (0.61, -1.05, 0.0),
    "YellowBag":  (0.95, -1.05, 0.0),
}


def _deselect():
    if bpy.context.mode != "OBJECT":
        bpy.ops.object.mode_set(mode="OBJECT")
    bpy.ops.object.select_all(action="DESELECT")


def weld_label(short):
    """Re-fit one label onto its parcel and join it in. Safe to run again."""
    parcel = bpy.data.objects.get(f"Parcel_{short}_HY")
    if parcel is None:
        return False

    # Already welded: the parcel carries the label's material and there is no
    # separate label object left to join.
    label = bpy.data.objects.get(f"Label_{short}")
    if label is None:
        return False

    _deselect()

    # Park the parcel back where it was imported, with its animation off, so
    # the fit happens on a still box in a known place. Both are restored by
    # whoever stages the shot afterwards.
    parcel.animation_data_clear()
    parcel.location = IMPORT_SPOT.get(short, (0.0, -1.05, 0.0))
    parcel.rotation_euler = IMPORT_ROT
    parcel.hide_render = parcel.hide_viewport = False
    bpy.context.view_layer.update()

    # Throw the old decal away and draw a fresh one against the parcel where
    # it now stands. Re-using the existing plane would carry over whatever
    # wrong offset it already had.
    bpy.data.objects.remove(label, do_unlink=True)
    if "apply" not in globals():
        raise RuntimeError("run apply_labels.py first - weld_labels needs apply()")
    label = globals()["apply"](parcel.name)
    if label is None:
        return False

    # Bake the shrinkwrap. A live modifier keeps re-projecting onto the
    # parcel's current surface, so the label slides across the box as it moves.
    bpy.context.view_layer.objects.active = label
    label.select_set(True)
    for mod in list(label.modifiers):
        bpy.ops.object.modifier_apply(modifier=mod.name)

    # Join. The label becomes a second material slot on the parcel, and from
    # here the two cannot be separated by any transform.
    _deselect()
    label.select_set(True)
    parcel.select_set(True)
    bpy.context.view_layer.objects.active = parcel
    bpy.ops.object.join()
    _deselect()
    return True


def weld_labels(shorts=None):
    done = [s for s in (shorts or list(IMPORT_SPOT)) if weld_label(s)]
    print(f"labels welded into their parcels: {len(done)} — {', '.join(done) or 'none'}")
    return done


if __name__ == "__main__":
    weld_labels()
