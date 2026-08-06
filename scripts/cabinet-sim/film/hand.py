"""A wooden mannequin forearm and mitten hand, for the film shots.

The scene already has a wooden artist's figure (`Person_HY`), but it is one
rigid 250k-vertex mesh with no armature, so its arm cannot be bent. This
builds a separate arm that can be keyframed.

**It is a mitten on purpose.** No fingers, no knuckles, no skin. The whole
film is shot with the wooden drawing figure, so a realistic hand would be the
one thing in frame pretending to be real. A mitten reads as "a hand" in a
quarter of a second and never invites the audience to look closer.

Run it through build_cabinet.py's collection convention:
    exec(open(r'scripts/cabinet-sim/film/hand.py').read()); build_hand()
"""

import bpy
import math

COLLECTION = "Film"

# Wrist at the origin, hand pointing +Y, palm facing down. Metres.
FOREARM_LEN = 0.26
WRIST_R = 0.036
ELBOW_R = 0.052

WOOD = (0.522, 0.353, 0.196, 1.0)   # sampled to sit beside Person_HY


def _collection():
    col = bpy.data.collections.get(COLLECTION)
    if col is None:
        col = bpy.data.collections.new(COLLECTION)
        bpy.context.scene.collection.children.link(col)
    return col


def _wood_material():
    """Turned beech, the colour of a drawing mannequin.

    This is built rather than borrowed. `Person_HY`'s material looks like the
    right wood in the viewport, but the colour is a 4096px baked texture atlas
    of that body - reusing it maps slices of torso onto these spheres.
    """
    mat = bpy.data.materials.get("Mat_Wood")
    if mat:
        return mat

    mat = bpy.data.materials.new("Mat_Wood")
    mat.use_nodes = True
    nt = mat.node_tree
    bsdf = nt.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = WOOD
    bsdf.inputs["Roughness"].default_value = 0.62
    bsdf.inputs["Metallic"].default_value = 0.0

    # Faint grain. Enough that the light moving over the arm has something to
    # catch; not so much that it becomes a pattern anyone reads.
    tex = nt.nodes.new("ShaderNodeTexNoise")
    tex.inputs["Scale"].default_value = 6.0
    tex.inputs["Detail"].default_value = 8.0
    tex.location = (-600, 200)

    ramp = nt.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].position = 0.40
    ramp.color_ramp.elements[0].color = (0.470, 0.310, 0.168, 1)
    ramp.color_ramp.elements[1].position = 0.62
    ramp.color_ramp.elements[1].color = WOOD
    ramp.location = (-380, 200)

    nt.links.new(tex.outputs["Fac"], ramp.inputs["Fac"])
    nt.links.new(ramp.outputs["Color"], bsdf.inputs["Base Color"])
    return mat


def _link(obj, mat, parent):
    for col in list(obj.users_collection):
        col.objects.unlink(obj)
    _collection().objects.link(obj)
    obj.data.materials.clear()
    obj.data.materials.append(mat)
    for poly in obj.data.polygons:
        poly.use_smooth = True
    obj.parent = parent
    return obj


def _blob(name, at, scale, rot_z=0.0):
    """A smooth ellipsoid. Every soft part of the arm is one of these."""
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=16, radius=1.0)
    obj = bpy.context.active_object
    obj.name = name
    obj.location = at
    obj.scale = scale
    obj.rotation_euler = (0, 0, math.radians(rot_z))
    return obj


def _taper(name, at, r_bottom, r_top, depth, rot_x=0.0):
    bpy.ops.mesh.primitive_cone_add(vertices=32, radius1=r_bottom, radius2=r_top,
                                    depth=depth, location=at)
    obj = bpy.context.active_object
    obj.name = name
    obj.rotation_euler = (math.radians(rot_x), 0, 0)
    return obj


def build_hand(name="Hand"):
    """Build the arm and return the empty that drives it.

    Everything is parented to one empty at the wrist, so the arm moves as a
    single rigid piece. A four second shot does not need an elbow that bends,
    and a rig nobody animates is a rig nobody maintains.
    """
    old = bpy.data.objects.get(name)
    if old:
        for child in list(old.children_recursive):
            bpy.data.objects.remove(child, do_unlink=True)
        bpy.data.objects.remove(old, do_unlink=True)

    root = bpy.data.objects.new(name, None)
    root.empty_display_type = "PLAIN_AXES"
    root.empty_display_size = 0.08
    _collection().objects.link(root)

    mat = _wood_material()

    # Forearm runs back from the wrist, thickening toward the elbow. The cone
    # is built along Z and laid down along -Y.
    _link(_taper(f"{name}_Forearm", (0, -FOREARM_LEN / 2 - 0.02, 0),
                 ELBOW_R, WRIST_R, FOREARM_LEN, rot_x=-90), mat, root)

    _link(_blob(f"{name}_Elbow", (0, -FOREARM_LEN - 0.02, 0),
                (ELBOW_R, ELBOW_R, ELBOW_R)), mat, root)

    # The wrist ball is what makes it read as a jointed figure rather than a
    # sock. It is the one detail the drawing-mannequin language depends on.
    _link(_blob(f"{name}_Wrist", (0, 0, 0),
                (WRIST_R, WRIST_R * 0.8, WRIST_R)), mat, root)

    _link(_blob(f"{name}_Palm", (0, 0.058, 0),
                (0.048, 0.058, 0.022)), mat, root)

    # One mass for all four fingers. This is the mitten. It is set forward far
    # enough to leave a crease at the knuckle and made a little thinner than
    # the palm, so there is a step in the silhouette. Without those two the
    # whole hand renders as a single blob and reads as a club.
    _link(_blob(f"{name}_Mitten", (0, 0.147, -0.003),
                (0.046, 0.044, 0.017)), mat, root)

    # The thumb is on +X because that is the side the camera is on. It is the
    # one feature that says "hand" rather than "limb", so it is never allowed
    # to sit on the far side of the mass.
    _link(_blob(f"{name}_Thumb", (0.050, 0.055, 0.004),
                (0.023, 0.040, 0.018), rot_z=-24), mat, root)

    return root


if __name__ == "__main__":
    build_hand()
    print("Hand built. Move/keyframe the 'Hand' empty; the arm follows.")
