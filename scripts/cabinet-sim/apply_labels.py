"""Stick a readable shipping label on each parcel prop.

Hunyuan bakes text into the texture as unreadable mush - inherent to how it
reconstructs from a photo, and regenerating does not fix it. So the label goes
on as a separate flat plane, drawn by make_label.py, floating just above the
parcel's top face.

Run inside Blender after the parcels are imported:
    exec(open(r'scripts/cabinet-sim/apply_labels.py').read())
"""
import os

import bpy
from mathutils import Vector

HERE = os.path.dirname(os.path.abspath(__file__)) if "__file__" in dir() else \
    r"C:\Users\admin\Project Postman\scripts\cabinet-sim"
PROPS = os.path.join(HERE, "props")

# Fraction of the parcel's top face the label covers. It must be generous:
# the decal has to bury the gibberish Hunyuan baked into the texture, not sit
# beside it. Anything under ~0.8 leaves the old label showing at the edges.
COVERAGE = 0.94
LABEL_RATIO = 640 / 1000.0        # make_label.py renders 1000 x 640


def _label_material(name, png):
    mat = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    bsdf = nt.nodes["Principled BSDF"]
    bsdf.inputs["Roughness"].default_value = 0.75
    if "Metallic" in bsdf.inputs:
        bsdf.inputs["Metallic"].default_value = 0.0
    tex = nt.nodes.new("ShaderNodeTexImage")
    tex.image = bpy.data.images.load(png, check_existing=True)
    tex.interpolation = "Cubic"
    tex.location = (-400, 200)
    nt.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    return mat


def _top_of(ob):
    """World-space centre of the parcel's top face, and its x/y extent."""
    pts = [ob.matrix_world @ Vector(c) for c in ob.bound_box]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    centre = Vector(((lo.x + hi.x) / 2, (lo.y + hi.y) / 2, hi.z))
    return centre, (hi.x - lo.x), (hi.y - lo.y)


def apply(parcel_name, png=None):
    ob = bpy.data.objects.get(parcel_name)
    if ob is None:
        print("missing parcel:", parcel_name)
        return None

    short = parcel_name.replace("Parcel_", "").replace("_HY", "")
    png = png or os.path.join(PROPS, f"label_{short}.png")
    if not os.path.exists(png):
        print("missing label:", png)
        return None

    centre, w, d = _top_of(ob)

    # The text always runs along world x, so somebody standing at the front of
    # the cabinet reads it left to right. Rotating the label to follow the
    # parcel's long side looks tidier in a render and is useless to a person.
    width = min(w, d / LABEL_RATIO) * COVERAGE
    height = width * LABEL_RATIO

    name = f"Label_{short}"
    old = bpy.data.objects.get(name)
    if old:
        bpy.data.objects.remove(old, do_unlink=True)

    # Build the quad by hand at the origin. primitive_plane_add + a baked
    # transform_apply folds the spawn offset into the mesh, which puts the
    # geometry somewhere the object's own location does not admit to.
    hw, hh = width / 2, height / 2
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata([(-hw, -hh, 0), (hw, -hh, 0), (hw, hh, 0), (-hw, hh, 0)],
                     [], [(0, 1, 2, 3)])
    uvs = [(0, 0), (1, 0), (1, 1), (0, 1)]
    mesh.uv_layers.new(name="UVMap")
    for loop, uv in zip(mesh.uv_layers[0].data, uvs):
        loop.uv = uv
    mesh.update()

    plane = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(plane)

    plane.data.materials.append(_label_material(f"{name}_mat", png))

    # Left unparented, in world space, on purpose. Hunyuan imports each parcel
    # rotated 90 degrees on X, so any parenting scheme maps the label into that
    # rotated frame and lands it on edge. Move a parcel and re-run apply_all().
    plane.location = centre + Vector((0, 0, 0.0015))   # 1.5 mm proud
    print(f"{name}: {round(width*1000)} x {round(height*1000)} mm on {parcel_name}")
    return plane


def apply_all():
    made = []
    for ob in sorted([o for o in bpy.data.objects
                      if o.name.startswith("Parcel_") and o.name.endswith("_HY")],
                     key=lambda o: o.name):
        p = apply(ob.name)
        if p:
            made.append(p.name)
    print(f"labels applied: {len(made)}")
    return made


apply_all()
