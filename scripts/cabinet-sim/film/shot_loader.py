"""The short shot: a parcel goes into box 04 and the door shuts.

This is the app's start-up loader. Four and a half seconds, portrait, no
words. The wooden figure pushes a parcel straight into an open box, lets go,
withdraws, and the door swings shut. The last frames settle onto the app's own
background colour so the interface draws over it with no visible seam.

Two rules the shot is built on:

**Straight in, not across.** The hand travels along one axis, into the box.
An arc looked like reaching around something, and it swung the parcel through
the open door's edge.

**The whole figure, never a floating arm.** A disembodied hand reads as a
ghost. The figure is a wooden artist's mannequin - no face, no fingers - so
there is nothing pretending to be a real person.

It is shot on box **04** because the app says 04 everywhere: the Opened
screen, the prototype, DESIGN.md. One box number, one product.

    exec(open(r'scripts/cabinet-sim/film/shot_loader.py').read())
    setup(); render()
"""

import bpy
import math

from mathutils import Vector

COLLECTION = "Film"

BOX = 4
# The app's background, so the last frame of the video IS the first frame of
# the interface. If DESIGN.md moves this colour, move it here in the same
# change or the loader will end on a visible step.
HOUSING = (0.812, 0.796, 0.761)     # #CFCBC2; see DESIGN.md

FPS = 30
DURATION_S = 3.4
LAST = int(FPS * DURATION_S)         # 102
RES = (720, 1280)                    # 9:16, phone native, small file

PARCEL = "Parcel_ShopeeBox_HY"
OPEN_ANGLE = 105                     # matches build_cabinet.OPEN_ANGLE

# --- The carry, described by where the PARCEL is ------------------------
#
# (frame, x, y, bottom-of-parcel z). The parcel is what the audience watches
# and the thing that has to land on the shelf, so it is what the path
# describes; the wrist that carries it is solved from this. Doing it the other
# way round - a fixed offset from the wrist - is what left the box floating
# 200 mm above the shelf, because how far the hand sits from the wrist depends
# on the arm's pose and changes all the way through the reach.
#
# **It goes in at mid-aperture, not at the lip.** Box 04's opening runs from
# z 1.44 to 1.77. Travelling in at 1.44 dragged the wrist and forearm straight
# through the bottom edge of the hole and the door frame. 1.56 clears it by
# 120 mm and still leaves room above.
CARRY = [
    # Chest height, not shoulder. At 1.43 the box crossed the figure's head in
    # frame - they do not overlap in space, but the camera looks slightly down
    # and projects one onto the other, which reads as the box growing out of
    # its neck. Starting low also gives the move somewhere to travel.
    (1,   0.580, -0.660, 1.270),     # held at chest, clear of the head
    (7,   0.580, -0.715, 1.262),     # anticipation: a small pull back first
    (36,  0.580, -0.060, 1.560),     # lifted, and driven in at mid-height
    (45,  0.580, -0.020, 1.440),     # lowered onto the shelf
]
RELEASE = 48                          # frame the parcel stops following

# The wrist, after it lets go. In wrist space now, because there is no longer
# a parcel to describe.
WITHDRAW = [
    (56,  0.580, -0.200, 1.610),     # lift off the box before pulling back
    (72,  0.580, -0.660, 1.560),     # straight back out, the way it came
    (86,  0.614, -0.627, 0.790),     # arm back down at the figure's side
]

# The parcel's origin is its own bottom face, so it sits this far above the
# middle of the hand bone while it is being carried.
PARCEL_LIFT = 0.022

# The door only starts to close once the hand is clear in y. Swinging it while
# the arm was still on its way out drove the door through the wrist - the door
# sweeps through x 0.29 to 0.77 as it shuts, and the hand withdraws at 0.58.
DOOR_CLOSE = (76, 96)
FADE_START, FADE_END = 96, LAST

# The camera has to sit to the right. The door is hinged on the left edge of
# the opening and swings out to the front left, so from the left it stands in
# front of the very thing the shot is about.
#
# Distance is set to hold the whole cabinet width and the figure from head to
# knee. Closer than about three metres and a 9:16 frame crops both at once,
# which reads as a mistake rather than as a close-up.
LOOK_AT = (0.40, -0.25, 1.16)
CAM_DIST = 3.20
CAM_DIR = (0.60, -0.75, 0.26)        # from the target, out toward the lens


def _col():
    col = bpy.data.collections.get(COLLECTION)
    if col is None:
        col = bpy.data.collections.new(COLLECTION)
        bpy.context.scene.collection.children.link(col)
    return col


def _door():
    return bpy.data.objects[f"Door_{BOX:02d}"]


def _cell_bounds():
    """World bounds of the box 04 opening: x0, x1, z0, z1.

    Measured from the door's rest position - its own location plus its local
    mesh - never from `matrix_world`. The door is swung open through most of
    this shot, and reading its world bounds while it stands open returns the
    rectangle the door is occupying in mid-air, not the hole in the cabinet.
    That put the shelf in the wrong place and landed the parcel a quarter of a
    metre left of the opening.
    """
    d = _door()
    xs = [v.co.x for v in d.data.vertices]
    zs = [v.co.z for v in d.data.vertices]
    return (d.location.x + min(xs), d.location.x + max(xs),
            d.location.z + min(zs), d.location.z + max(zs))


def _key(obj, frame, loc=None, rot_z=None):
    if loc is not None:
        obj.location = loc
        obj.keyframe_insert("location", frame=frame)
    if rot_z is not None:
        obj.rotation_euler.z = math.radians(rot_z)
        obj.keyframe_insert("rotation_euler", index=2, frame=frame)


def _shelf():
    """A shelf for box 04 to stand the parcel on.

    The simulator has no internal shelves - it was built to answer *which door
    opened*, and a shelf would not have changed that answer. A parcel needs
    somewhere to land, so the film adds one, in the film's own collection.
    This is set dressing. It is not a change to the cabinet.
    """
    name = "Film_Shelf_04"
    old = bpy.data.objects.get(name)
    if old:
        bpy.data.objects.remove(old, do_unlink=True)

    x0, x1, z0, _ = _cell_bounds()
    mesh = bpy.data.meshes.new(name)
    y0, y1, t = -0.205, 0.200, 0.012
    verts = [(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0 - t, z0)]
    faces = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1),
             (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    obj.data.materials.append(bpy.data.materials["Mat_Frame"])
    _col().objects.link(obj)
    return obj


def _ground():
    """A floor and a back wall in the app's own colour.

    Not a studio infinity curve - a corner. The cabinet needs a surface behind
    it or the open door has nothing to cast a shadow onto, and that shadow is
    what says the door is standing away from the cabinet.
    """
    name = "Film_Ground"
    old = bpy.data.objects.get(name)
    if old:
        bpy.data.objects.remove(old, do_unlink=True)

    mat = bpy.data.materials.get("Mat_Housing") or bpy.data.materials.new("Mat_Housing")
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = (*HOUSING, 1)
    bsdf.inputs["Roughness"].default_value = 0.88
    bsdf.inputs["Metallic"].default_value = 0.0

    mesh = bpy.data.meshes.new(name)
    verts = [(-6, -6, 0), (6, -6, 0), (6, 3.2, 0), (-6, 3.2, 0),
             (-6, 3.2, 6), (6, 3.2, 6)]
    mesh.from_pydata(verts, [], [(0, 1, 2, 3), (3, 2, 5, 4)])
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    obj.data.materials.append(mat)
    _col().objects.link(obj)
    return obj


def _camera():
    """Three-quarter view: the figure in profile and the open box beside it.

    Straight on, an open door is a black rectangle and reads as a hole in a
    wall. From here the door is visible standing open, and the figure's reach
    crosses the frame rather than pointing into it.
    """
    target = bpy.data.objects.get("Film_Target")
    if target is None:
        target = bpy.data.objects.new("Film_Target", None)
        target.empty_display_type = "SPHERE"
        target.empty_display_size = 0.05
        _col().objects.link(target)
    target.location = LOOK_AT

    cam = bpy.data.objects.get("Film_Cam")
    if cam is None:
        cam = bpy.data.objects.new("Film_Cam", bpy.data.cameras.new("Film_Cam"))
        _col().objects.link(cam)

    cam.data.lens = 40
    cam.data.dof.use_dof = True
    cam.data.dof.focus_distance = CAM_DIST
    # f/6.3, not f/2.8. Shot wide open, the door, the number and the parcel
    # all softened at once; at 720 px wide that is not depth, it is a smear.
    cam.data.dof.aperture_fstop = 6.3

    cam.location = tuple(LOOK_AT[i] + CAM_DIST * CAM_DIR[i] for i in range(3))
    # Aimed by constraint, not by three angles worked out by hand. Moving
    # LOOK_AT now re-aims the camera on its own.
    cam.rotation_euler = (0, 0, 0)
    cam.constraints.clear()
    track = cam.constraints.new("TRACK_TO")
    track.target = target
    track.track_axis = "TRACK_NEGATIVE_Z"
    track.up_axis = "UP_Y"

    bpy.context.scene.camera = cam
    return cam


def _lights():
    """A key from the front left, a cool rim from the right, and one small
    fill inside the box so the parcel does not vanish as it goes in."""
    for name in ("Film_Key", "Film_Rim", "Film_BoxFill"):
        old = bpy.data.objects.get(name)
        if old:
            bpy.data.objects.remove(old, do_unlink=True)

    def area(name, loc, rot, size, energy, colour):
        light = bpy.data.lights.new(name, "AREA")
        light.size = size
        light.energy = energy
        light.color = colour
        obj = bpy.data.objects.new(name, light)
        obj.location = loc
        obj.rotation_euler = [math.radians(a) for a in rot]
        _col().objects.link(obj)
        return obj

    area("Film_Key", (-1.2, -3.0, 3.3), (44, 0, -20), 3.0, 620, (1.0, 0.97, 0.93))
    area("Film_Rim", (3.2, -0.9, 2.6), (68, 0, 108), 1.6, 320, (0.86, 0.91, 1.0))
    # Small and close, so it lights the parcel and not the whole carcass.
    area("Film_BoxFill", (0.58, 0.16, 1.62), (-90, 0, 0), 0.30, 14, (1.0, 0.95, 0.88))


def _palm():
    """Where the middle of the hand is, right now, in world space.

    IK drives the *wrist*; the hand runs a further 165 mm past it in whatever
    direction the arm ended up pointing. So the parcel's position is measured
    off the solved rig at each keyframe instead of being a fixed offset from
    the target - a guessed offset put the box through the figure's chest at
    the start of the shot and floating in mid-air at the end.
    """
    arm = bpy.data.objects["Person_Rig"]
    bpy.context.view_layer.update()
    bone = arm.pose.bones["Hand.R"]
    return (arm.matrix_world @ bone.matrix) @ Vector((0.0, bone.length * 0.55, 0.0))


def _wrist_for(x, y, parcel_bottom):
    """Where the wrist has to be for the parcel to sit at `parcel_bottom`.

    Solved rather than assumed. IK drives the wrist, the hand runs a further
    165 mm past it, and how much of that is height depends on the arm's pose -
    so a fixed offset is right in one frame and wrong in the next. This nudges
    the wrist until the measured palm puts the parcel where it is wanted, which
    converges in three or four steps because palm height rises with wrist
    height over the range the arm actually moves through.
    """
    hand = bpy.data.objects["IK_Hand"]
    z = parcel_bottom + 0.02
    for _ in range(10):
        hand.location = (x, y, z)
        error = parcel_bottom - (_palm().z + PARCEL_LIFT)
        if abs(error) < 0.0015:
            break
        z += error
    return (x, y, z)


def _fcurves(obj):
    """Every F-curve on an object, on old and new action layouts alike.

    Blender 4.4 moved F-curves out of `action.fcurves` and into
    layers -> strips -> channelbags. Reading only the old place finds nothing
    on 5.x and every easing call silently does nothing.
    """
    ad = obj.animation_data
    if not ad or not ad.action:
        return []
    action = ad.action
    found = []
    for layer in getattr(action, "layers", []):
        for strip in getattr(layer, "strips", []):
            for bag in getattr(strip, "channelbags", []):
                found.extend(bag.fcurves)
    return found or list(action.fcurves)


def _ease(obj, frame, interpolation="CUBIC", easing="EASE_OUT", path=None):
    """Shape the curve leaving one keyframe.

    Blender's interpolation belongs to the segment *after* a key, the same way
    a GSAP tween's ease belongs to the tween it is declared on. So an arrival
    that should decelerate is EASE_OUT on the key it departs from.

    Default is a cubic ease-out - GSAP's `power3.out` - because almost
    everything here is a hand or a door arriving somewhere and stopping.
    """
    for curve in _fcurves(obj):
        if path and curve.data_path != path:
            continue
        for key in curve.keyframe_points:
            if abs(key.co.x - frame) < 0.5:
                key.interpolation = interpolation
                key.easing = easing


def _animate():
    door = _door()
    hand = bpy.data.objects["IK_Hand"]
    parcel = bpy.data.objects[PARCEL]
    for obj in (door, hand, parcel):
        obj.animation_data_clear()

    # The door is already open when the shot starts. It has to be: the film is
    # four and a half seconds and the story is the parcel, not the mechanism.
    _key(door, 1, rot_z=-OPEN_ANGLE)
    _key(door, DOOR_CLOSE[0], rot_z=-OPEN_ANGLE)
    # Swings fast, then latches. It cannot overshoot past zero - that would
    # drive the door through the carcass - so the settle is on the near side:
    # most of the travel happens quickly, and the last seven degrees take a
    # fifth of a second. That reads as weight, where a single linear sweep
    # reads as a door on a spring.
    _key(door, DOOR_CLOSE[1] - 6, rot_z=-7)
    _key(door, DOOR_CLOSE[1], rot_z=0)
    _ease(door, DOOR_CLOSE[0], "QUINT", "EASE_IN")       # slow to start moving
    _ease(door, DOOR_CLOSE[1] - 6, "SINE", "EASE_OUT")   # then ease into the latch

    # The shelf, read from the door's own geometry so it cannot drift out of
    # agreement with the cabinet the way a typed-in height would.
    _, _, shelf_top, _ = _cell_bounds()

    # Solve every wrist position first, then key. Once an object owns an
    # F-curve the animation system rewrites its location on each depsgraph
    # update, so solving and keying in one pass reads back poses that the
    # keyframes just overwrote.
    beats = []
    for frame, x, y, z in CARRY:
        want = shelf_top if frame == CARRY[-1][0] else z
        beats.append((frame, _wrist_for(x, y, want), (x, y, want)))

    for frame, wrist, at in beats:
        _key(hand, frame, loc=wrist)
        _key(parcel, frame, loc=at)

    # After the release the parcel holds still. Two identical keys, so nothing
    # drifts while the hand pulls away.
    rest = beats[-1][2]
    _key(parcel, RELEASE, loc=rest)
    _key(parcel, LAST, loc=rest)

    for frame, x, y, z in WITHDRAW:
        _key(hand, frame, loc=(x, y, z))

    # --- The timing ----------------------------------------------------
    #
    # Four ideas, borrowed straight from how a GSAP timeline would be built:
    #
    #   anticipation - a small move against the main one before it starts
    #   power3.out   - fast off the mark, decelerating into the target
    #   power2.inOut - a travel move that both starts and stops softly
    #   settle       - the last few millimetres taken slowly
    #
    # Nothing here is linear. Constant velocity is the single thing that makes
    # animation read as a machine playing back keyframes.
    anticipation, drive, place = CARRY[0][0], CARRY[1][0], CARRY[2][0]
    _ease(hand, anticipation, "SINE", "EASE_IN_OUT")     # ease into the pull-back
    _ease(hand, drive, "CUBIC", "EASE_OUT")              # then commit, and settle
    _ease(hand, place, "SINE", "EASE_IN_OUT")            # lower it gently
    _ease(parcel, anticipation, "SINE", "EASE_IN_OUT")
    _ease(parcel, drive, "CUBIC", "EASE_OUT")
    _ease(parcel, place, "SINE", "EASE_IN_OUT")

    _ease(hand, CARRY[-1][0], "SINE", "EASE_IN_OUT")     # let go, lift off
    _ease(hand, WITHDRAW[0][0], "CUBIC", "EASE_IN_OUT")  # pull back out
    _ease(hand, WITHDRAW[1][0], "SINE", "EASE_IN_OUT")   # arm drops to the side


def _fade_out():
    """Fade the last half second into the app's own background colour.

    Done in the compositor rather than with a plane in front of the lens, so
    it cannot be knocked out of alignment by a camera move and does not care
    what the render engine does with transparency.

    Written against Blender 5.x, where the compositor is a node group hung off
    `scene.compositing_node_group`. The 4.x route - `scene.node_tree`, a
    `CompositorNodeComposite` output and `CompositorNodeMixRGB` - is gone:
    none of those three names exist any more.
    """
    scene = bpy.context.scene
    name = "Loader_Fade"

    old = bpy.data.node_groups.get(name)
    if old:
        bpy.data.node_groups.remove(old)
    ng = bpy.data.node_groups.new(name, "CompositorNodeTree")

    layers = ng.nodes.new("CompositorNodeRLayers")
    layers.location = (-520, 0)
    layers.scene = scene

    flat = ng.nodes.new("CompositorNodeRGB")
    flat.location = (-520, -240)
    flat.outputs[0].default_value = (*HOUSING, 1)

    mix = ng.nodes.new("ShaderNodeMixRGB")
    mix.location = (-160, 0)
    ng.links.new(layers.outputs["Image"], mix.inputs["Color1"])
    ng.links.new(flat.outputs["Color"], mix.inputs["Color2"])

    ng.interface.new_socket("Image", in_out="OUTPUT", socket_type="NodeSocketColor")
    out = ng.nodes.new("NodeGroupOutput")
    out.location = (120, 0)
    ng.links.new(mix.outputs["Color"], out.inputs[0])

    fac = mix.inputs["Factor"]
    fac.default_value = 0.0
    fac.keyframe_insert("default_value", frame=FADE_START)
    fac.default_value = 1.0
    fac.keyframe_insert("default_value", frame=FADE_END)

    scene.compositing_node_group = ng
    scene.use_nodes = True


def setup():
    """Build the set, frame it and animate it. Does not render."""
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.fps = FPS
    scene.render.resolution_x, scene.render.resolution_y = RES
    scene.render.resolution_percentage = 100
    scene.frame_start, scene.frame_end = 1, LAST
    scene.render.film_transparent = False

    # The loader is shot in a studio corner, not at the campus gate. If
    # scene_gate.py has been run this session its canopy posts stand in front
    # of the cabinet and its forecourt replaces the app-coloured ground, so
    # the fade at the end lands on the wrong colour.
    #
    # Looked up in bpy.data rather than by calling scene_gate's show_gate().
    # These files are exec'd, not imported, so whether one can see another's
    # functions depends on how the caller set up its namespace - and it
    # silently did not here, which left the canopy standing in the shot.
    gate = bpy.data.collections.get("Gate")
    if gate:
        for obj in gate.objects:
            obj.hide_render = obj.hide_viewport = True

    scene.world.use_nodes = True
    scene.view_settings.view_transform = "AgX"
    scene.view_settings.look = "None"
    scene.view_settings.exposure = 0.0
    bg = scene.world.node_tree.nodes["Background"]
    # If a sky HDRI has been loaded for the long film it is wired into this
    # socket, and a socket with a link ignores its default value entirely -
    # so setting the colour here would silently do nothing and the loader
    # would be lit by a sunset.
    for link in list(scene.world.node_tree.links):
        if link.to_socket == bg.inputs["Color"]:
            scene.world.node_tree.links.remove(link)
    bg.inputs["Color"].default_value = (0.55, 0.56, 0.58, 1)
    bg.inputs["Strength"].default_value = 0.6

    # Only the parcel being delivered is in shot. staging.py owns where the
    # rest live - a corner behind the cabinet - so this no longer carries a
    # hand-written list of names that goes stale the moment a prop is added.
    for short in ("BlackWrap", "BubbleWrap", "PolyMailer", "Tube", "YellowBag"):
        spare = bpy.data.objects.get(f"Parcel_{short}_HY")
        if spare:
            spare.hide_render = spare.hide_viewport = True
    hero = bpy.data.objects[PARCEL]
    hero.hide_render = hero.hide_viewport = False

    _ground()
    _shelf()
    _camera()
    _lights()
    _animate()
    _fade_out()
    scene.frame_set(58)
    print(f"Loader shot ready: {LAST} frames at {FPS} fps, {RES[0]}x{RES[1]}.")


def render(out_path=None):
    """Render straight to an MP4.

    Blender carries its own ffmpeg, which is the only one on this machine -
    there is none on PATH - so encoding happens here rather than as a later
    step over a folder of PNGs.
    """
    scene = bpy.context.scene
    out = out_path or bpy.path.abspath("//out/loader")

    # Blender 5.x splits stills from video: FFMPEG is not in the file_format
    # enum at all until media_type says VIDEO. Setting file_format first
    # raises `enum "FFMPEG" not found`.
    scene.render.image_settings.media_type = "VIDEO"
    scene.render.image_settings.file_format = "FFMPEG"
    scene.render.ffmpeg.format = "MPEG4"
    scene.render.ffmpeg.codec = "H264"
    # Small file matters: this plays while the app is starting up.
    scene.render.ffmpeg.constant_rate_factor = "HIGH"
    scene.render.ffmpeg.ffmpeg_preset = "GOOD"
    scene.render.ffmpeg.audio_codec = "NONE"
    scene.render.filepath = out

    bpy.ops.render.render(animation=True)
    print(f"Rendered {LAST} frames to {out}.mp4")


if __name__ == "__main__":
    setup()
