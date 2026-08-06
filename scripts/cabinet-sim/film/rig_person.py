"""Give the wooden mannequin an arm that bends, and stick the labels down.

`Person_HY` arrives as one solid 250k-vertex mesh with no bones and no loose
parts, so nothing on it can move. The film needs it to reach into a box.

**Segments are rigid, not skinned.** A real artist's mannequin is turned wood
pieces on ball joints - no part of it bends or stretches. So every vertex is
assigned to exactly one bone at full weight, cut at the waists already
modelled into the mesh, and a wooden ball is dropped over each cut to cover
the seam. Smooth skinning would pinch the elbow into a rubber hose and make
the one thing the audience is watching look wrong.

The arm is driven by inverse kinematics: move the `IK_Hand` empty and the
shoulder and elbow work themselves out. That is what makes a straight-in
reach a straight line in the file as well as on screen.

    exec(open(r'scripts/cabinet-sim/film/rig_person.py').read())
    rig()
"""

import bpy
import numpy as np
from mathutils import Vector

FIGURE = "Person_HY"
ARM = "Person_Rig"
COLLECTION = "Film"

# --- Measured off the mesh, in the figure's own local space -----------------
# Local X is left-right, Y is up, Z is back: the toes run to -Z. The joint
# heights are the waists in the arm's radius profile, so the cuts land where
# the model already narrows and the seam has somewhere to hide.
Y_WRIST = 0.542
Y_ELBOW = 0.730
Y_SHOULDER = 0.945

# How far from the arm's own centreline a vertex can be and still be arm.
# A plain "x beyond 0.098" test does not work: at hip height the pelvis is as
# wide as the hanging arm, so that test dragged half the hip up with the hand
# and stretched a strand of mesh from the shoulder to the floor. Distance to
# the centreline separates them, because the hip is nowhere near it.
ARM_REACH = 0.050
HAND_REACH = 0.058     # the hand is a flat paddle, wider than the wrist

# Joint centres (x, y, z) and the ball radius that covers each cut.
JOINTS = {
    "shoulder": ((0.121, Y_SHOULDER, 0.007), 0.032),
    "elbow":    ((0.136, Y_ELBOW,    0.007), 0.030),
    "wrist":    ((0.147, Y_WRIST,    0.005), 0.025),
}
FINGERTIP = (0.147, 0.429, 0.004)

# Where the figure stands: in front of box 04's column, facing the cabinet.
# Rotation (90, 0, 0) puts local +Y up and local -Z - the front - toward +Y,
# which is the cabinet. Y is set so the box is just inside arm's reach.
STAND_AT = (0.40, -0.62, 0.0)
STAND_ROT = (np.pi / 2, 0.0, 0.0)


def _col():
    col = bpy.data.collections.get(COLLECTION)
    if col is None:
        col = bpy.data.collections.new(COLLECTION)
        bpy.context.scene.collection.children.link(col)
    return col


def stick_labels():
    """Parent each shipping label to the parcel it is stuck to.

    apply_labels.py leaves them unparented in world space on purpose - the
    shrinkwrap needs that while it is being fitted - with a note to re-run it
    after moving a parcel. A film moves a parcel every frame, so instead the
    modifier is applied and the label becomes part of the parcel's transform.
    Without this the parcel flies into the locker and its label stays on the
    floor, which is exactly what the first render did.
    """
    stuck = []
    for label in [o for o in bpy.data.objects if o.name.startswith("Label_")
                  and not o.name[6:8].isdigit()]:
        parcel = bpy.data.objects.get(
            f"Parcel_{label.name.replace('Label_', '')}_HY")
        if parcel is None or label.parent is not None:
            continue

        # Freeze the wrap first. A shrinkwrap that keeps evaluating will drag
        # the label back onto the parcel's *current* surface every frame, so
        # once the parcel moves the label slides across it.
        for mod in list(label.modifiers):
            bpy.context.view_layer.objects.active = label
            bpy.ops.object.modifier_apply(modifier=mod.name)

        label.parent = parcel
        label.matrix_parent_inverse = parcel.matrix_world.inverted()
        stuck.append(label.name)
    print(f"labels stuck down: {len(stuck)}")
    return stuck


def _place_figure():
    fig = bpy.data.objects[FIGURE]
    fig.location = STAND_AT
    fig.rotation_euler = STAND_ROT
    fig.hide_render = False
    fig.hide_viewport = False
    bpy.context.view_layer.update()
    return fig


def _world(fig, local):
    return fig.matrix_world @ Vector(local)


def _labels(L):
    """Per-vertex segment: 0 body, 1 upper arm, 2 forearm, 3 hand."""
    segments = [
        (1, JOINTS["shoulder"][0], JOINTS["elbow"][0], ARM_REACH),
        (2, JOINTS["elbow"][0],    JOINTS["wrist"][0], ARM_REACH),
        (3, JOINTS["wrist"][0],    FINGERTIP,          HAND_REACH),
    ]
    out = np.zeros(len(L), dtype=np.int8)
    for tag, head, tail, reach in segments:
        a, b = np.array(head), np.array(tail)
        ab = b - a
        t = np.clip(((L - a) @ ab) / (ab @ ab), 0.0, 1.0)[:, None]
        dist = np.linalg.norm(L - (a + t * ab), axis=1)
        out[(dist < reach) & (out == 0)] = tag
    return out


def _components(bm):
    """Connected-component id per vertex, by union-find over the edges."""
    n = len(bm.verts)
    parent = np.arange(n)

    def find(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a

    for e in bm.edges:
        ra, rb = find(e.verts[0].index), find(e.verts[1].index)
        if ra != rb:
            parent[ra] = rb
    return np.array([find(i) for i in range(n)])


def separate_segments(fig):
    """Cut the figure into rigid pieces at the joints, and cap the cuts.

    Hunyuan reconstructs from photographs, so wherever two surfaces touch it
    welds them: this figure's arm is fused to its hip down the whole length of
    the forearm, and the head is one shell with the body. Lifting the arm
    therefore dragged a sheet of hip up with it - on screen, a wooden rod from
    the shoulder to the floor.

    No weighting scheme fixes that, because the geometry really is joined. The
    mesh has to be cut. Which is also what the object being modelled is: a
    real artist's mannequin is separate turned pieces on ball joints, and the
    balls added afterwards cover every cut.
    """
    import bmesh

    me = fig.data
    # Cutting twice is not harmless. The caps this adds sit exactly on the
    # joint boundary, so on a second pass they read as their own segment, get
    # cut off and re-capped, and the mesh grows every time rig() is called.
    if me.get("segments_cut"):
        print("  already cut")
        return
    n = len(me.vertices)
    co = np.empty(n * 3, dtype=np.float32)
    me.vertices.foreach_get("co", co)
    lab = _labels(co.reshape(n, 3))

    bm = bmesh.new()
    bm.from_mesh(me)
    bm.verts.ensure_lookup_table()
    bm.faces.ensure_lookup_table()

    def owner(face):
        """A face belongs to whichever segment holds most of its corners."""
        tags = [lab[v.index] for v in face.verts]
        return max(set(tags), key=tags.count)

    was_open = {e.index for e in bm.edges if len(e.link_faces) == 1}
    seam = [e for e in bm.edges
            if len(e.link_faces) == 2
            and owner(e.link_faces[0]) != owner(e.link_faces[1])]
    if not seam:
        print("  already separated")
        bm.free()
        return

    bmesh.ops.split_edges(bm, edges=seam)

    # Cap both sides of every cut, so each piece stays a closed solid. An open
    # tube shows its own inside surface the moment the arm turns.
    holes = [e for e in bm.edges
             if len(e.link_faces) == 1 and e.index not in was_open]
    if holes:
        bmesh.ops.holes_fill(bm, edges=holes, sides=0)

    bm.to_mesh(me)
    bm.free()
    me.update()
    me["segments_cut"] = True
    print(f"  cut {len(seam)} edges, capped {len(holes)} openings")


def _assign_groups(fig):
    """One bone per vertex, full weight. No blending anywhere.

    Done with numpy over the raw coordinate array rather than with
    bpy.ops selection: 250k vertices through the operator layer takes minutes
    and depends on what happens to be selected when it runs.
    """
    for g in list(fig.vertex_groups):
        fig.vertex_groups.remove(g)

    import bmesh

    n = len(fig.data.vertices)
    co = np.empty(n * 3, dtype=np.float32)
    fig.data.vertices.foreach_get("co", co)
    L = co.reshape(n, 3)

    # Assign by connected piece, not by position. After the cut the seam has
    # two vertices at the same coordinate - one on each side - so a positional
    # test cannot tell them apart and would put both on the same bone,
    # re-welding the arm to the body it was just cut from.
    bm = bmesh.new()
    bm.from_mesh(fig.data)
    bm.verts.ensure_lookup_table()
    comp = _components(bm)
    bm.free()

    names = {0: "Body", 1: "UpperArm.R", 2: "Forearm.R", 3: "Hand.R"}
    masks = {v: np.zeros(n, dtype=bool) for v in names.values()}

    for cid in np.unique(comp):
        mask = comp == cid
        if mask.sum() > n * 0.4:          # the trunk, head and legs
            masks["Body"] |= mask
            continue
        mid = L[mask][:, 1].mean()
        if mid >= Y_ELBOW:
            key = "UpperArm.R"
        elif mid >= Y_WRIST:
            key = "Forearm.R"
        elif mid >= 0.40:
            key = "Hand.R"
        else:
            key = "Body"                  # anything else the cut shook loose
        masks[key] |= mask

    idx = np.arange(n)
    for name, mask in masks.items():
        group = fig.vertex_groups.new(name=name)
        group.add(idx[mask].tolist(), 1.0, "REPLACE")
        print(f"  {name}: {int(mask.sum())} verts")


def _build_armature(fig):
    old = bpy.data.objects.get(ARM)
    if old:
        bpy.data.objects.remove(old, do_unlink=True)

    arm_data = bpy.data.armatures.new(ARM)
    arm = bpy.data.objects.new(ARM, arm_data)
    _col().objects.link(arm)

    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode="EDIT")

    shoulder = _world(fig, JOINTS["shoulder"][0])
    elbow = _world(fig, JOINTS["elbow"][0])
    wrist = _world(fig, JOINTS["wrist"][0])
    tip = _world(fig, FINGERTIP)

    def bone(name, head, tail, parent=None, connected=False):
        b = arm_data.edit_bones.new(name)
        b.head, b.tail = head, tail
        if parent:
            b.parent = arm_data.edit_bones[parent]
            b.use_connect = connected
        return b

    # The body is one bone. Nothing below the shoulder needs to move in a shot
    # this short, and a spine nobody animates is a spine nobody maintains.
    bone("Body", _world(fig, (0, 0.50, 0)), _world(fig, (0, Y_SHOULDER, 0)))
    bone("UpperArm.R", shoulder, elbow, "Body")
    bone("Forearm.R", elbow, wrist, "UpperArm.R", connected=True)
    bone("Hand.R", wrist, tip, "Forearm.R", connected=True)

    bpy.ops.object.mode_set(mode="OBJECT")
    return arm, wrist, elbow


def _ik(arm, wrist, elbow):
    """Two-bone IK on the forearm, plus a pole so the elbow points backward."""
    def empty(name, at, size=0.05):
        e = bpy.data.objects.get(name)
        if e is None:
            e = bpy.data.objects.new(name, None)
            e.empty_display_type = "SPHERE"
            _col().objects.link(e)
        e.empty_display_size = size
        e.location = at
        e.animation_data_clear()
        return e

    hand_target = empty("IK_Hand", wrist)
    # Behind and below the elbow. An arm reaching forward should have its
    # elbow drop and trail, not wing out sideways like a chicken.
    pole = empty("IK_Elbow", (elbow.x + 0.45, elbow.y - 0.55, elbow.z - 0.30), 0.08)

    pb = arm.pose.bones["Forearm.R"]
    for c in list(pb.constraints):
        pb.constraints.remove(c)
    ik = pb.constraints.new("IK")
    ik.target = hand_target
    ik.pole_target = pole
    ik.pole_angle = 0.0
    ik.chain_count = 2
    return hand_target, pole


def _balls(fig, arm):
    """A turned wooden ball over each cut, so the seam never shows.

    This is what the real toy does. Rigid segments have to gap when they
    rotate, and a ball at the joint is the honest fix rather than a skinning
    trick that would make the wood look like rubber.
    """
    mat = bpy.data.materials.get("Mat_Wood")
    if mat is None:
        raise RuntimeError("Mat_Wood is missing - run hand.py first.")

    owner = {"shoulder": "UpperArm.R", "elbow": "UpperArm.R", "wrist": "Forearm.R"}
    scale = fig.scale[0]
    for joint, (local, radius) in JOINTS.items():
        name = f"Joint_{joint}"
        old = bpy.data.objects.get(name)
        if old:
            bpy.data.objects.remove(old, do_unlink=True)

        bpy.ops.mesh.primitive_uv_sphere_add(
            segments=24, ring_count=12, radius=radius * scale,
            location=_world(fig, local))
        ball = bpy.context.active_object
        ball.name = name
        for c in list(ball.users_collection):
            c.objects.unlink(ball)
        _col().objects.link(ball)
        ball.data.materials.append(mat)
        for poly in ball.data.polygons:
            poly.use_smooth = True

        # Bone parenting hangs the child off the bone's TAIL, not its head, so
        # an inverse worked out from the pose matrix throws the ball a bone's
        # length away - one of them ended up floating beside the cabinet.
        # Record the world position, parent, then put it back.
        where = ball.matrix_world.copy()
        ball.parent = arm
        ball.parent_type = "BONE"
        ball.parent_bone = owner[joint]
        bpy.context.view_layer.update()
        ball.matrix_world = where


def rig():
    """Rig the figure and return the empty that drives its hand."""
    if bpy.context.mode != "OBJECT":
        bpy.ops.object.mode_set(mode="OBJECT")
    bpy.ops.object.select_all(action="DESELECT")

    stick_labels()
    fig = _place_figure()
    separate_segments(fig)
    _assign_groups(fig)
    arm, wrist, elbow = _build_armature(fig)

    for mod in list(fig.modifiers):
        fig.modifiers.remove(mod)
    mod = fig.modifiers.new("Rig", "ARMATURE")
    mod.object = arm
    mod.use_bone_envelopes = False
    mod.use_vertex_groups = True

    hand_target, _ = _ik(arm, wrist, elbow)
    _balls(fig, arm)

    bpy.context.view_layer.update()
    print(f"Rigged. Move '{hand_target.name}' and the arm follows.")
    return hand_target


if __name__ == "__main__":
    rig()
