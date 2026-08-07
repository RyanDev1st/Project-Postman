"""A tidy corner to keep every prop in when it is not in a shot.

The parcels imported from Hunyuan landed in a row on the floor directly in
front of the cabinet, which is exactly where the camera points. Every shot
then had to hide them one by one by name, and any shot that forgot showed six
parcels lying on the ground outside a locker.

So there is one parking area, well behind the cabinet and out of every
sightline, and one call that puts everything back in it:

    exec(open(r'scripts/cabinet-sim/film/staging.py').read())
    park_all()                 # everything back on the shelf
    fetch("ShopeeBox", (0.58, -0.6, 1.5))   # take one out for a shot

`park_all()` is safe to run at any time and is the first thing a shot should
do, so a shot starts from a known stage rather than from whatever the last
one left behind.
"""

import bpy

COLLECTION = "Staging"

# Behind the cabinet and off to the left. The cabinet's back is at y = +0.225
# and the gate wall at y = +1.6, so this sits behind both. Nothing in the film
# looks this way.
ORIGIN = (-3.6, 3.4, 0.0)
SPACING = 0.55

PARCELS = ["BlackWrap", "BubbleWrap", "PolyMailer", "ShopeeBox", "Tube", "YellowBag"]

# Where the figure waits when it is not acting. Far enough from the parcels
# that its 1.7 m of mannequin does not stand in the middle of them.
FIGURE_PARK = (-6.0, 3.4, 0.0)


def _col():
    col = bpy.data.collections.get(COLLECTION)
    if col is None:
        col = bpy.data.collections.new(COLLECTION)
        bpy.context.scene.collection.children.link(col)
    return col


def _marker():
    """A visible outline round the parking area, so it is obvious in the
    viewport that the corner is deliberate and not somewhere props drifted."""
    name = "Staging_Area"
    old = bpy.data.objects.get(name)
    if old:
        bpy.data.objects.remove(old, do_unlink=True)

    obj = bpy.data.objects.new(name, None)
    obj.empty_display_type = "CUBE"
    obj.empty_display_size = 1.0
    obj.location = (ORIGIN[0] + SPACING * 2.5, ORIGIN[1], 0.5)
    obj.scale = (2.6, 1.0, 0.6)
    obj.hide_render = True          # a guide, never in a frame
    _col().objects.link(obj)
    return obj


def parcel(short):
    return bpy.data.objects.get(f"Parcel_{short}_HY")


def park(short, slot):
    """Put one parcel on the shelf, in its own slot, and show it.

    Its label rides along - the labels are parented to their parcels, which
    is what stopped them staying on the floor when a parcel moved.
    """
    obj = parcel(short)
    if obj is None:
        return None
    obj.location = (ORIGIN[0] + slot * SPACING, ORIGIN[1], ORIGIN[2])
    obj.hide_render = obj.hide_viewport = False
    return obj


def fetch(short, at, hide_others=True):
    """Take one parcel out for a shot, and put the rest away.

    Returns the object so a shot can keyframe it. `hide_others` is on by
    default because a shot almost always wants exactly one parcel, and the
    other five sitting on a shelf behind the wall still cost render time.
    """
    if hide_others:
        for other in PARCELS:
            if other == short:
                continue
            obj = parcel(other)
            if obj:
                obj.hide_render = obj.hide_viewport = True

    obj = parcel(short)
    if obj is None:
        raise KeyError(f"no parcel {short!r}. Known: {', '.join(PARCELS)}")
    obj.location = at
    obj.hide_render = obj.hide_viewport = False
    return obj


def park_figure():
    fig = bpy.data.objects.get("Person_HY")
    if fig:
        fig.location = FIGURE_PARK
    return fig


def park_all(include_figure=False):
    """Everything back in the corner. Safe to run at any time.

    The figure stays where it is unless asked for: `rig_person.rig()` stands
    it in front of the cabinet, and parking it by default would undo that
    every time a shot reset its props.
    """
    _marker()
    for slot, short in enumerate(PARCELS):
        park(short, slot)
    if include_figure:
        park_figure()

    # Move them into the Staging collection so the whole corner can be hidden
    # in one click, and so it is obvious in the outliner where props live.
    col = _col()
    for short in PARCELS:
        obj = parcel(short)
        if obj is None:
            continue
        for old in list(obj.users_collection):
            if old is not col:
                old.objects.unlink(obj)
        if obj.name not in col.objects:
            col.objects.link(obj)

    print(f"parked {len(PARCELS)} parcels at {ORIGIN}")


if __name__ == "__main__":
    park_all()
