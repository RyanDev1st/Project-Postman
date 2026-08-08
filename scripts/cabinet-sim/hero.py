"""A product shot of the cabinet, for the app to put on a screen.

The app needs to show the receiver *the cabinet they are walking to*, with
their own door marked. A flat drawing of a grid of squares was tried and
rejected - it reads as a diagram, and a diagram of a locker is not worth
looking at.

So this renders the real cabinet, the one `build_cabinet.py` builds from the
workshop drawing, as a low-key product shot on a transparent background. The
app puts its own ground behind it.

**It also exports where every door landed on screen.** Rendering one image per
door state would be 19 renders and a rebuild every time a door changes hands.
Instead the door corners are projected through the render camera into
normalised screen coordinates, and written next to the image. The app then
draws the glow itself, over the render, on whichever door it needs. One
render, any state.

    "A:\\Blender Foundation\\Blender 5.1\\blender.exe" -b cabinet_sim.blend \\
        --python hero.py

Output lands in `hero/`: `cabinet.png` and `cabinet.json`.
"""

import json
import math
import os

import bpy
from mathutils import Vector

from bpy_extras.object_utils import world_to_camera_view

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "hero")

# A three-quarter view. Ryan picked it over the flat elevation on 2026-08-07:
# straight on, the cabinet is a chart of rectangles, and the angle is what
# makes it look like an object worth walking to.
#
# The flat version was built to keep the app's push-into-a-door move square,
# and that reason is real but small - a door near the middle of the grid barely
# skews, and the move reads as a camera pushing in rather than a rectangle
# scaling, which is arguably the better of the two. The projection export in
# `_export_doors` works from whatever camera is set, so the highlight lands
# correctly either way.
RES_X, RES_Y = 2100, 2100
SAMPLES = 220

# --- the light ------------------------------------------------------------
# These were measured, not chosen. The doors are galvanised steel at 0.60
# albedo; the first attempt lit them at 1400 W and every one came back at 0.95
# luminance - a flat white slab with no tonal range, which is what makes a
# render look like cheap plastic. `preview()` samples the rendered pixels and
# prints where they actually land, and these are the numbers that put the door
# faces in the mid-tones with the highlights and the shadow side apart.
KEY_ENERGY = 300.0
RIM_ENERGY = 240.0
FILL_ENERGY = 26.0
EXPOSURE = -0.72

# The rim's colour. Pulled back from a saturated blue: at (0.35, 0.58, 1.00)
# it painted the cabinet's whole right side an even blue and the steel read as
# blue acrylic. A rim is meant to draw an edge, not to dye a panel, so this is
# barely tinted - enough to tie the shot to the app's accent, not enough to
# argue about what the cabinet is made of.
RIM_COLOUR = (0.62, 0.75, 1.0)

# The cabinet, in metres. Mirrors build_cabinet.py.
CAB_H = 1.800
CENTRE = Vector((0.0, 0.0, 1.000))

# A long lens, well back. A wide lens close up bows the door grid outward and
# makes a 1.6 m cabinet look like a vending machine.
LENS = 105.0
CAM_DIST = 6.6
CAM_YAW = math.radians(26.0)    # off the front axis, enough to see the side
                                # panel. This much angle foreshortens the far
                                # column, which is affordable now the digits
                                # are 100 mm instead of 72
CAM_Z = 1.02                    # near level. A tall object shot level reads
                                # architectural; tilting up reads amateur

COLLECTION = "Cabinet"

# Door numbers, in millimetres. Must match `label.data.size` in
# build_cabinet.py. This script works on an already-built cabinet in the saved
# blend rather than rebuilding it, so the size has to be re-applied here or the
# render keeps whatever the blend was saved with.
NUMBER_MM = 100.0


def _clear_props():
    """Everything that is not the cabinet stays out of the frame.

    The blend also holds the film set - a campus gate, a staging corner of
    parcels, a mannequin. Hiding by name breaks the moment one is renamed, so
    this hides everything and then shows the cabinet back.
    """
    for obj in bpy.data.objects:
        obj.hide_render = True

    cab = bpy.data.collections.get(COLLECTION)
    if cab is None:
        raise RuntimeError(f"no {COLLECTION!r} collection - run build_cabinet.py first")
    for obj in cab.objects:
        obj.hide_render = False
    return cab


def _shut_every_door():
    """A shot of the cabinet is a shot of it closed. An open door in the hero
    image would say something the app has not been told yet.

    Setting the rotation is not enough. The loader film left keyframes on door
    04, and an animated channel is re-evaluated on every frame - the first
    render came back with that door swung wide despite the assignment above
    it. The animation has to go before the pose will hold.
    """
    for obj in bpy.data.objects:
        if obj.name.startswith("Door_"):
            obj.animation_data_clear()
            obj.rotation_euler = (0.0, 0.0, 0.0)
            obj["state"] = "shut"


def _numbers():
    """Make the stencilled door numbers readable at phone size.

    The app draws this cabinet about 300 px wide. A number that is legible on
    the real steel from a metre away is four or five pixels there, which is
    the difference between "my box is that one" and a grey smudge.

    Two changes: the digits are larger, and the paint is pure matte black
    rather than near-black with a metallic component. A metallic dark on a
    lit metal door picks up the same highlights as the door and the contrast
    collapses exactly where the light is strongest.
    """
    grown = 0
    for obj in bpy.data.objects:
        if not obj.name.startswith("Label_") or obj.type != "FONT":
            continue
        obj.data.size = NUMBER_MM / 1000.0
        obj.data.extrude = 0.0022
        obj.data.offset = 0.0016
        grown += 1

    paint = bpy.data.materials.get("number")
    if paint and paint.use_nodes:
        bsdf = paint.node_tree.nodes.get("Principled BSDF")
        if bsdf:
            bsdf.inputs["Base Color"].default_value = (0.006, 0.006, 0.008, 1)
            bsdf.inputs["Roughness"].default_value = 0.86
            if "Metallic" in bsdf.inputs:
                bsdf.inputs["Metallic"].default_value = 0.0
    print(f"door numbers set to {NUMBER_MM:.0f} mm: {grown}")


def _finish(cab):
    """Break every edge, and take the doors from matte to satin.

    Two changes, and between them they are most of the difference between a
    render that reads as a photograph and one that reads as a CG box.

    **The bevel.** Nothing in the world has a perfectly sharp 90 degree edge.
    Real sheet metal has a radius on every fold, and that radius catches a
    thin bright line along the whole length of the door. Without it the eye
    gets a mathematically perfect corner, which is the single loudest tell
    that a picture was computed.

    **The roughness.** At 0.55 the doors blur the studio into one flat value,
    so the softbox next door reflects as nothing at all. At 0.30 the same
    metal holds a soft image of it and the door faces gain a gradient. This
    is a finish choice - satin powder coat rather than matte - and it belongs
    to the shot, not to the drawing, which is why it is applied here as a
    modifier and a material tweak rather than edited into build_cabinet.py.
    """
    for obj in cab.objects:
        if obj.type != "MESH":
            continue
        if not any(m.type == "BEVEL" for m in obj.modifiers):
            bevel = obj.modifiers.new("Hero_Bevel", "BEVEL")
            bevel.width = 0.0018          # 1.8 mm, the fold on a steel panel
            bevel.segments = 2
            bevel.limit_method = "ANGLE"
            bevel.angle_limit = math.radians(40)
            bevel.harden_normals = True
        # harden_normals needs shading to be smooth to have anything to do.
        for poly in obj.data.polygons:
            poly.use_smooth = True

    door = bpy.data.materials.get("Mat_Door")
    if door and door.use_nodes:
        bsdf = door.node_tree.nodes.get("Principled BSDF")
        if bsdf:
            bsdf.inputs["Roughness"].default_value = 0.30


def _light(name, where, energy, size, colour=(1.0, 1.0, 1.0)):
    data = bpy.data.lights.new(name, type="AREA")
    data.energy = energy
    data.size = size
    data.color = colour
    obj = bpy.data.objects.new(name, data)
    obj.location = where
    bpy.context.scene.collection.objects.link(obj)

    aim = obj.constraints.new("TRACK_TO")
    aim.target = _target()
    aim.track_axis = "TRACK_NEGATIVE_Z"
    aim.up_axis = "UP_Y"
    return obj


_TARGET_NAME = "Hero_Aim"


def _target():
    """One empty at the cabinet's middle, and everything points at it.

    Hand-written euler angles were wrong three times on the film before this
    was learnt: aiming is a constraint's job, not arithmetic done by eye.
    """
    old = bpy.data.objects.get(_TARGET_NAME)
    if old:
        return old
    obj = bpy.data.objects.new(_TARGET_NAME, None)
    obj.location = CENTRE
    obj.empty_display_size = 0.2
    bpy.context.scene.collection.objects.link(obj)
    return obj


def _camera():
    name = "Hero_Cam"
    old = bpy.data.objects.get(name)
    if old:
        bpy.data.objects.remove(old, do_unlink=True)

    data = bpy.data.cameras.new(name)
    data.lens = LENS
    obj = bpy.data.objects.new(name, data)
    obj.location = (
        math.sin(CAM_YAW) * CAM_DIST,
        -math.cos(CAM_YAW) * CAM_DIST,
        CAM_Z,
    )
    bpy.context.scene.collection.objects.link(obj)

    # Aiming is a constraint's job. Hand-written euler angles were wrong three
    # times on the film before that was learnt.
    aim = obj.constraints.new("TRACK_TO")
    aim.target = _target()
    aim.track_axis = "TRACK_NEGATIVE_Z"
    aim.up_axis = "UP_Y"

    bpy.context.scene.camera = obj
    return obj


def _stage():
    """Three lights and a floor that only catches shadow.

    Low key on purpose. The doors are galvanised steel at 0.60 albedo, which
    under flat light renders as a pale grey slab - accurate and lifeless. Lit
    dark, the same paint keeps its real value relationship but the highlights
    do the describing. Nothing here repaints the cabinet; it is the lighting
    that makes it look like metal worth photographing.
    """
    for name in ("Hero_Key", "Hero_Rim", "Hero_Fill", "Hero_Floor", "Hero_Softbox"):
        old = bpy.data.objects.get(name)
        if old:
            bpy.data.objects.remove(old, do_unlink=True)

    # Key: high and well to the left, raking across the doors rather than
    # facing them. A light square-on lights every door identically and the
    # cabinet flattens into a chart; raked, the grid gains a gradient from the
    # hinge side to the handle side and the doors read as separate panels.
    #
    # (The flat-elevation version of this shot could not use a rake at all -
    # under an orthographic camera every point on a flat face shares a normal
    # and a view direction, so one attempt put 58% of the render on a single
    # value. Perspective gets the gradient for free, which is part of why the
    # angle looks less bland.)
    _light("Hero_Key", (-4.2, -3.4, 4.1), energy=KEY_ENERGY, size=3.4)

    # Rim: behind and right, in the app's own accent blue. This is the one
    # deliberate liberty - it ties the render to the interface it sits in, and
    # the lit edge it draws down the cabinet's far side is what separates the
    # object from a dark background. An angled view has a silhouette for it to
    # work on, which a flat elevation does not.
    _light("Hero_Rim", (3.6, 2.2, 2.4), energy=RIM_ENERGY, size=1.6,
           colour=RIM_COLOUR)

    # Fill: low, front-right, barely there. Lifts the shadow side off black
    # without ever competing with the key.
    _light("Hero_Fill", (2.6, -3.6, 0.6), energy=FILL_ENERGY, size=2.5)

    # A floor that catches the contact shadow and renders nothing else.
    #
    # There is no wall behind. The flat elevation needed one - with no
    # silhouette and no visible sides, a cast shadow was the only cue that the
    # cabinet had depth at all. At an angle the object describes its own depth,
    # and a wall set far enough back to clear the silhouette lands inside the
    # frame instead: the first angled render came back with a grey band down
    # the right-hand side, which was the wall.
    #
    # A shadow catcher under a transparent film renders as alpha alone, so the
    # app still gets a cut-out cabinet with a real shadow under its feet.
    bpy.ops.mesh.primitive_plane_add(size=16, location=(0, 0, 0))
    floor = bpy.context.active_object
    floor.name = "Hero_Floor"
    floor.is_shadow_catcher = True

    _softbox()
    _world_gradient()


def _softbox():
    """A tall bright panel to the left, there to be reflected.

    This is the difference between metal and grey paint. The doors are
    metallic 0.7, and a metal surface renders by reflecting whatever is around
    it - in a flat black world there is nothing to reflect, so it falls back
    to a matte value and the whole cabinet reads as cardboard. No exposure
    setting repairs that, because the missing thing is not light, it is
    something to see.

    A strip rather than a square, because a strip reflects as a vertical band
    running down the door faces, and a band that slides across a form is what
    describes it.
    """
    old = bpy.data.objects.get("Hero_Softbox")
    if old:
        bpy.data.objects.remove(old, do_unlink=True)

    bpy.ops.mesh.primitive_plane_add(size=1.0, location=(-2.5, -2.5, 1.5))
    box = bpy.context.active_object
    box.name = "Hero_Softbox"
    box.scale = (0.45, 4.6, 1.0)
    box.rotation_euler = (math.radians(90), 0.0, math.radians(-42))

    mat = bpy.data.materials.new("Hero_SoftboxMat")
    mat.use_nodes = True
    tree = mat.node_tree
    for node in list(tree.nodes):
        if node.type != "OUTPUT_MATERIAL":
            tree.nodes.remove(node)
    emit = tree.nodes.new("ShaderNodeEmission")
    emit.inputs["Color"].default_value = (1.0, 0.98, 0.95, 1.0)
    emit.inputs["Strength"].default_value = 9.0
    tree.links.new(emit.outputs["Emission"],
                   tree.nodes["Material Output"].inputs["Surface"])
    box.data.materials.append(mat)

    # Seen by reflections, never by the camera. In shot it would be a white
    # slab floating beside the cabinet.
    box.visible_camera = False
    box.visible_shadow = False
    return box


def _world_gradient():
    """A dark sky that is lighter at the top, not a flat void.

    Gives the cabinet's top edges and its steel sides a graded reflection, and
    stops the shadow side going to dead black.
    """
    world = bpy.context.scene.world
    if world is None:
        world = bpy.data.worlds.new("Hero_World")
        bpy.context.scene.world = world
    world.use_nodes = True
    tree = world.node_tree

    for node in list(tree.nodes):
        if node.type != "OUTPUT_WORLD":
            tree.nodes.remove(node)

    coord = tree.nodes.new("ShaderNodeTexCoord")
    mapping = tree.nodes.new("ShaderNodeMapping")
    mapping.inputs["Rotation"].default_value = (math.radians(90), 0.0, 0.0)
    gradient = tree.nodes.new("ShaderNodeTexGradient")
    ramp = tree.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].position = 0.32
    ramp.color_ramp.elements[0].color = (0.004, 0.005, 0.007, 1.0)
    ramp.color_ramp.elements[1].position = 0.92
    ramp.color_ramp.elements[1].color = (0.055, 0.075, 0.115, 1.0)
    bg = tree.nodes.new("ShaderNodeBackground")
    bg.inputs["Strength"].default_value = 1.0

    tree.links.new(coord.outputs["Generated"], mapping.inputs["Vector"])
    tree.links.new(mapping.outputs["Vector"], gradient.inputs["Vector"])
    tree.links.new(gradient.outputs["Color"], ramp.inputs["Fac"])
    tree.links.new(ramp.outputs["Color"], bg.inputs["Color"])
    tree.links.new(bg.outputs["Background"],
                   tree.nodes["World Output"].inputs["Surface"])


def _render_settings(res_x=RES_X, res_y=RES_Y, samples=SAMPLES):
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.samples = samples
    scene.cycles.use_denoising = True
    scene.render.resolution_x = res_x
    scene.render.resolution_y = res_y
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True

    # Blender 5.1 gates file_format behind media_type. The film left this
    # blend in VIDEO, where the only format on offer is FFMPEG, so a still
    # cannot be asked for until the media type is a still.
    scene.render.image_settings.media_type = "IMAGE"
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"

    scene.view_settings.view_transform = "AgX"
    scene.view_settings.look = "AgX - Medium High Contrast"
    scene.view_settings.exposure = EXPOSURE

    os.makedirs(OUT, exist_ok=True)
    scene.render.filepath = os.path.join(OUT, "cabinet.png")


def _front_face(obj):
    """The four corners of a door's front face, in world space.

    Read off the object's own bounding box rather than recomputed from the
    drawing. The doors are hinged on their left edge, so their origin is not
    their centre and arithmetic from the grid would be quietly wrong.

    The cabinet faces -Y, so the front face is the four corners with the
    smallest Y.
    """
    corners = [obj.matrix_world @ Vector(c) for c in obj.bound_box]
    front_y = min(c.y for c in corners)
    face = [c for c in corners if abs(c.y - front_y) < 1e-4]
    if len(face) != 4:
        return None

    # Wind them: top-left, top-right, bottom-right, bottom-left, as seen from
    # in front. From -Y, screen-left is +X.
    top = sorted([c for c in face if c.z > sum(f.z for f in face) / 4],
                 key=lambda c: -c.x)
    bottom = sorted([c for c in face if c.z <= sum(f.z for f in face) / 4],
                    key=lambda c: -c.x)
    if len(top) != 2 or len(bottom) != 2:
        return None
    return [top[0], top[1], bottom[1], bottom[0]]


def _project(scene, cam, point):
    """World point to normalised screen, 0..1, y measured down from the top."""
    uv = world_to_camera_view(scene, cam, point)
    return [round(uv.x, 5), round(1.0 - uv.y, 5)]


def _export_doors(cam):
    """Where each door landed in the render, so the app can light it up."""
    scene = bpy.context.scene
    doors = {}
    for obj in bpy.data.objects:
        if not obj.name.startswith("Door_"):
            continue
        number = obj.get("box_number")
        if number is None:
            continue
        face = _front_face(obj)
        if face is None:
            continue
        doors[int(number)] = [_project(scene, cam, c) for c in face]

    data = {
        "image": "cabinet.png",
        "size": [RES_X, RES_Y],
        "note": (
            "Door corners projected through the render camera. "
            "Normalised 0..1, origin top left. Regenerate with hero.py "
            "whenever the camera or the drawing changes."
        ),
        "doors": {str(k): doors[k] for k in sorted(doors)},
    }
    path = os.path.join(OUT, "cabinet.json")
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=1)
    print(f"projected {len(doors)} doors -> {path}")
    return doors


def _report():
    """Where the cabinet's pixels actually landed, in tenths of luminance.

    Looking at a render and calling it "too bright" is how three lighting
    passes were wasted on the campus gate. A histogram says whether the metal
    has a tonal range or is one flat value, which is the difference between a
    photograph and a chart.

    Transparent pixels are dropped: the background is empty here, and counting
    it would drag every number toward black.
    """
    # Read the file back, not "Render Result". In a background run that buffer
    # comes back empty - the first attempt reported "nothing opaque in frame"
    # against an image that plainly had a cabinet in it.
    path = bpy.context.scene.render.filepath
    if not os.path.exists(path):
        print(f"report: no file at {path}")
        return
    image = bpy.data.images.load(path, check_existing=False)
    pixels = list(image.pixels)
    bpy.data.images.remove(image)

    buckets = [0] * 10
    lit = 0
    for i in range(0, len(pixels), 4):
        if pixels[i + 3] < 0.5:
            continue
        # Rec. 709 luminance, on the display-referred values.
        y = 0.2126 * pixels[i] + 0.7152 * pixels[i + 1] + 0.0722 * pixels[i + 2]
        buckets[min(9, max(0, int(y * 10)))] += 1
        lit += 1

    if not lit:
        print("report: nothing opaque in frame")
        return

    bars = " ".join(f"{n / lit * 100:4.1f}" for n in buckets)
    print(f"luminance 0.0-1.0 in tenths, % of {lit} lit pixels:\n  {bars}")


def preview(scale=0.22, samples=48):
    """A fast look, with the numbers, for dialling the light."""
    cab = _clear_props()
    _shut_every_door()
    _numbers()
    _finish(cab)
    _stage()
    _camera()
    _render_settings(res_x=int(RES_X * scale), res_y=int(RES_Y * scale),
                     samples=samples)
    bpy.context.scene.render.filepath = os.path.join(OUT, "preview.png")
    bpy.ops.render.render(write_still=True)
    _report()


def shoot():
    cab = _clear_props()
    _shut_every_door()
    _numbers()
    _finish(cab)
    _stage()
    cam = _camera()
    _render_settings()

    bpy.context.view_layer.update()
    doors = _export_doors(cam)
    if len(doors) != 19:
        print(f"WARNING: projected {len(doors)} doors, the drawing has 19")

    bpy.ops.render.render(write_still=True)
    _report()
    print(f"hero render -> {bpy.context.scene.render.filepath}")


if __name__ == "__main__":
    import sys

    preview() if "--preview" in sys.argv else shoot()
