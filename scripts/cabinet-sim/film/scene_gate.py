"""The campus gate the long film is shot at.

A plausible Vietnamese university gate in late-afternoon light: a rendered
wall, gate piers, a road, planting, and the cabinet standing under a canopy.

**It is not a reconstruction of VGU's D9 backgate.** Nothing was measured and
no imagery was traced - see story.md. It is built from what is known about the
place: Ben Cat, Binh Duong, hard warm tropical light, palms and frangipani.
Replace it the day somebody takes photographs; it is one collection and one
function.

Everything lives in the `Gate` collection so the film can hide the whole
outdoors in one line for the shots that happen at the cabinet face.

    exec(open(r'scripts/cabinet-sim/film/scene_gate.py').read()); build_gate()
"""

import bpy
import math
import random

COLLECTION = "Gate"

# The cabinet is 1.6 m wide and stands at the origin, facing -Y. Everything
# here is placed around that, in metres.
WALL_Y = 1.6           # the wall runs behind the cabinet
ROAD_Y = -7.0          # the road edge, in front

# Measured-ish albedos, not "what the colour looks like on a screen".
#
# The first pass used 0.70 for concrete and 0.76 for the wall, and the whole
# render came out as a milky white plane with no shadows readable on it. Real
# concrete reflects about a third of the light that hits it; sun-bleached
# paint about half. Under a filmic view transform those two numbers are the
# difference between a photograph and a whiteout.
PALETTE = {
    "wall":    (0.480, 0.455, 0.415, 1),   # painted render, sun-bleached
    "pier":    (0.400, 0.378, 0.340, 1),
    "cap":     (0.150, 0.145, 0.140, 1),   # dark capping stone
    "road":    (0.055, 0.055, 0.058, 1),   # asphalt is very dark
    "kerb":    (0.330, 0.322, 0.310, 1),   # concrete
    "grass":   (0.075, 0.130, 0.048, 1),
    "trunk":   (0.135, 0.105, 0.075, 1),
    "leaf":    (0.065, 0.150, 0.055, 1),
    "canopy":  (0.105, 0.112, 0.120, 1),
    "post":    (0.115, 0.120, 0.125, 1),
}


def _col():
    col = bpy.data.collections.get(COLLECTION)
    if col is None:
        col = bpy.data.collections.new(COLLECTION)
        bpy.context.scene.collection.children.link(col)
    return col


def _mat(name, rgba, rough=0.85):
    """Fetch or create a material, and always write the current values into it.

    It used to return early when the material already existed, which made
    `build_gate()` only look idempotent: it rebuilt every object but kept the
    materials from the first run. Editing the palette and rebuilding then
    changed nothing on screen, which reads as "the fix did not work" rather
    than "the fix was not applied".
    """
    mat = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = rgba
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Metallic"].default_value = 0.0
    return mat


def _box(name, size, at, mat, rot_z=0.0):
    sx, sy, sz = (v / 2 for v in size)
    verts = [(x * sx, y * sy, z * sz)
             for x in (-1, 1) for y in (-1, 1) for z in (-1, 1)]
    faces = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1),
             (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    obj.location = at
    obj.rotation_euler = (0, 0, rot_z)
    obj.data.materials.append(mat)
    _col().objects.link(obj)
    return obj


def _cyl(name, radius, depth, at, mat, rot=(0, 0, 0), verts=16):
    bpy.ops.mesh.primitive_cylinder_add(vertices=verts, radius=radius,
                                        depth=depth, location=at)
    obj = bpy.context.active_object
    obj.name = name
    obj.rotation_euler = rot
    for c in list(obj.users_collection):
        c.objects.unlink(obj)
    _col().objects.link(obj)
    obj.data.materials.append(mat)
    for p in obj.data.polygons:
        p.use_smooth = True
    return obj


def _ground():
    """Grass verge, road, and a kerb between them."""
    _box("Gate_Grass", (60, 40, 0.02), (0, WALL_Y - 20, -0.01),
         _mat("Mat_Grass", PALETTE["grass"], rough=0.95))
    _box("Gate_Forecourt", (26, 9, 0.03), (0, -2.6, 0.0),
         _mat("Mat_Kerb", PALETTE["kerb"], rough=0.9))
    _box("Gate_Road", (60, 9, 0.02), (0, ROAD_Y - 4.5, -0.005),
         _mat("Mat_Road", PALETTE["road"], rough=0.7))
    _box("Gate_Kerb", (60, 0.30, 0.16), (0, ROAD_Y, 0.06),
         _mat("Mat_Kerb", PALETTE["kerb"], rough=0.9))


def _wall():
    """A rendered boundary wall with a gap for the gate, and two piers."""
    wall = _mat("Mat_Wall", PALETTE["wall"], rough=0.92)
    cap = _mat("Mat_Cap", PALETTE["cap"], rough=0.8)
    pier = _mat("Mat_Pier", PALETTE["pier"], rough=0.9)

    # Left and right runs, with the gate opening between them.
    for tag, x, width in (("L", -12.0, 14.0), ("R", 12.0, 14.0)):
        _box(f"Gate_Wall_{tag}", (width, 0.25, 2.4), (x, WALL_Y, 1.2), wall)
        _box(f"Gate_Cap_{tag}", (width + 0.1, 0.36, 0.10), (x, WALL_Y, 2.45), cap)

    # The piers either side of the opening. Square, capped, slightly taller.
    for tag, x in (("L", -5.0), ("R", 5.0)):
        _box(f"Gate_Pier_{tag}", (0.7, 0.7, 3.0), (x, WALL_Y, 1.5), pier)
        _box(f"Gate_PierCap_{tag}", (0.9, 0.9, 0.14), (x, WALL_Y, 3.07), cap)


def _canopy():
    """A flat steel canopy over the cabinet. It exists for the light.

    Outdoors under a tropical sun the cabinet face blows out and the door
    numbers stop reading. The canopy puts it in shade, which is both what a
    real installation would need and what makes the shot legible.
    """
    post = _mat("Mat_Post", PALETTE["post"], rough=0.5)
    roof = _mat("Mat_Canopy", PALETTE["canopy"], rough=0.45)

    _box("Gate_Canopy", (3.6, 2.2, 0.08), (0, -0.35, 2.72), roof)
    for x in (-1.55, 1.55):
        for y in (-1.30, 0.55):
            _cyl(f"Gate_Post_{x}_{y}", 0.045, 2.68, (x, y, 1.34), post)


def _palm(name, at, height, lean=0.0):
    """A palm, suggested rather than modelled: a leaning trunk and a crown.

    At the distance these are shot from, a fan of angled blades reads as a
    palm and a detailed frond reads as noise. Detail nobody can resolve is
    render time spent on nothing.
    """
    trunk = _mat("Mat_Trunk", PALETTE["trunk"], rough=0.9)
    leaf = _mat("Mat_Leaf", PALETTE["leaf"], rough=0.85)

    _cyl(f"{name}_Trunk", 0.16, height, (at[0], at[1], height / 2),
         trunk, rot=(lean, 0, 0), verts=10)

    top = (at[0] + math.sin(lean) * 0, at[1] - math.sin(lean) * height / 2, height)
    for i in range(9):
        angle = (2 * math.pi / 9) * i
        droop = math.radians(58)
        blade = _box(
            f"{name}_Frond_{i}", (0.30, 2.5, 0.03),
            (top[0] + math.cos(angle) * 1.1,
             top[1] + math.sin(angle) * 1.1,
             top[2] - 0.30),
            leaf,
        )
        blade.rotation_euler = (droop * math.cos(angle), droop * math.sin(angle), angle)


def _planting():
    """Palms behind the wall and a hedge along it. Placed with a fixed seed.

    Random, but the same random every time - a scene that rearranges itself
    between renders makes two shots of the same place not match.
    """
    random.seed(7)
    # Well back behind the wall. At 2.6 m they crowded the top corner of a
    # wide shot and read as green rectangles rather than as trees - close
    # enough for the eye to ask what they were, not close enough to answer.
    for i, x in enumerate((-11.5, -7.4, 8.6, 12.5, 17.0, -16.5)):
        _palm(f"Gate_Palm_{i}", (x, WALL_Y + 7.5 + random.uniform(0, 3.0)),
              height=7.2 + random.uniform(-0.9, 1.6),
              lean=random.uniform(-0.07, 0.07))

    hedge = _mat("Mat_Leaf", PALETTE["leaf"], rough=0.85)
    for tag, x, width in (("L", -12.0, 13.6), ("R", 12.0, 13.6)):
        _box(f"Gate_Hedge_{tag}", (width, 0.55, 0.65), (x, WALL_Y - 0.45, 0.32), hedge)


def _sun():
    """Late afternoon, low and warm, raking across the cabinet face.

    A sun lamp, not just the HDRI, because the HDRI alone gives soft light
    with no shadow direction and the whole scene goes flat. The angle is
    chosen so the open door casts a shadow onto the cabinet - that shadow is
    what tells a viewer the door is standing open.
    """
    for name in ("Gate_Sun", "Gate_Bounce"):
        old = bpy.data.objects.get(name)
        if old:
            bpy.data.objects.remove(old, do_unlink=True)

    # Measured, not guessed. At 5.2 against a full-strength sky the render
    # came back with ground, wall and sky all inside 0.55-0.67 - not blown
    # out, just flat, because the ambient was drowning the sun and nothing
    # cast a shadow you could read. The sun has to win by a wide margin
    # outdoors; this is roughly a 10:1 key-to-fill, which is what midday
    # tropical sun actually looks like.
    sun = bpy.data.lights.new("Gate_Sun", "SUN")
    sun.energy = 13.0
    sun.color = (1.0, 0.93, 0.82)
    sun.angle = math.radians(1.5)          # a real sun is not a point
    obj = bpy.data.objects.new("Gate_Sun", sun)
    # A sun shines along its own -Z. At (56, 0, -118) that worked out to
    # light travelling from BEHIND the wall - the cabinet face and the whole
    # forecourt were backlit, which is why the first renders were flat and
    # blue no matter how the energy was raised. It was never an exposure
    # problem; the key light was pointing the wrong way.
    #
    # The cabinet faces -Y and the camera sits front-right, so the key comes
    # from front-left: light travels +X +Y and down, and the shadows fall
    # away from the lens.
    obj.rotation_euler = (math.radians(58), 0, math.radians(-25))
    _col().objects.link(obj)

    # Warm bounce off the forecourt, so the shaded side is not dead black.
    fill = bpy.data.lights.new("Gate_Bounce", "AREA")
    fill.size = 8.0
    fill.energy = 60
    fill.color = (1.0, 0.90, 0.78)
    fobj = bpy.data.objects.new("Gate_Bounce", fill)
    fobj.location = (1.6, -4.2, 1.1)
    fobj.rotation_euler = (math.radians(74), 0, math.radians(24))
    _col().objects.link(fobj)


def grade():
    """Exposure and contrast for the outdoor shots.

    Kept with the scene rather than left at Blender's defaults, so the film
    does not depend on whoever opened the file last. The sky is dimmed as a
    *light source* far more than it is as a backdrop: a full-strength HDRI
    lights everything from every direction at once, which is exactly what
    removes the shadows an outdoor shot needs.
    """
    scene = bpy.context.scene
    scene.world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.42
    scene.view_settings.view_transform = "AgX"
    scene.view_settings.look = "AgX - Medium High Contrast"
    # Set by measuring, not by eye. At +0.35 the wall - a 0.48 albedo - came
    # back at 0.91 on screen, which is nearly paper white for a painted
    # surface in afternoon sun. Concrete should land near 0.5, so the whole
    # thing comes down about a stop and a quarter.
    scene.view_settings.exposure = -0.85


def build_gate():
    """Build the whole outdoors. Safe to run again."""
    old = bpy.data.collections.get(COLLECTION)
    if old:
        for obj in list(old.objects):
            bpy.data.objects.remove(obj, do_unlink=True)
        bpy.data.collections.remove(old)

    _ground()
    _wall()
    _canopy()
    _planting()
    _sun()
    grade()

    # The loader shot's grey studio corner would sit in the middle of all
    # this. Hide it rather than delete it - the short film still needs it.
    for name in ("Film_Ground",):
        obj = bpy.data.objects.get(name)
        if obj:
            obj.hide_render = obj.hide_viewport = True

    print(f"Gate built: {len(_col().objects)} objects.")


def show_gate(visible=True):
    """Turn the outdoors on or off in one line."""
    col = bpy.data.collections.get(COLLECTION)
    if not col:
        return
    for obj in col.objects:
        obj.hide_render = not visible
        obj.hide_viewport = not visible


if __name__ == "__main__":
    build_gate()
