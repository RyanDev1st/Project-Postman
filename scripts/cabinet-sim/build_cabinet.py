"""VGU Smart Locker - simulated cabinet.

Builds the cabinet from the design drawing and exposes an event API so the
app's behaviour can be tested without hardware: open a door, shut it, change
the screen. See docs/adr/0008-cabinet-simulator.md.

THIS IS NOT EVIDENCE. A task whose Verify names the real cabinet is ticked by
the real cabinet, never by this. The simulator is for building; the cabinet is
for proving. Rule A5.

Run it either way:
    blender --python scripts/cabinet-sim/build_cabinet.py
    exec(open(r'scripts/cabinet-sim/build_cabinet.py').read())   # Scripting tab

Then drive it:
    open_door(4); close_door(4); set_screen('WELCOME'); reset()
"""

import bpy
import math

from mathutils import Matrix

# --- The drawing, in millimetres -------------------------------------------
# Every number here comes from the design sheet. Change these, not the code.
W, H, D = 1600, 1800, 450       # overall width, height, depth
FOOT = 60                        # floor to the bottom of the body
BORDER = 30                      # frame edge around the door grid
GAP = 8                          # gap between doors
COLS, ROWS = 4, 5
DOOR10_H = 110                   # the short door under the control panel
PANEL_COL, PANEL_ROWS = 1, (1, 2)   # panel sits in column 2, rows 2-3
SCREEN_IN = 21.5                 # touchscreen, diagonal inches, portrait

# Which door number sits at (row, column). None = the control panel.
# 06 is absent: the panel occupies it. That is the drawing, not a typo here.
# See assumption A-15 - the spec says 20 compartments, the drawing shows 19.
GRID = [
    [1,  2,    3,  4],
    [5,  None, 7,  8],
    [9,  10,   11, 12],
    [13, 14,   15, 16],
    [17, 18,   19, 20],
]

BODY_H = H - FOOT
CELL_W = (W - 2 * BORDER - (COLS - 1) * GAP) / COLS
CELL_H = (BODY_H - 2 * BORDER - (ROWS - 1) * GAP) / ROWS

COLLECTION = "Cabinet"
OPEN_ANGLE = 105                 # how far a door swings, degrees

PALETTE = {                      # from MATERIALS & FINISH on the drawing
    "frame": (0.055, 0.060, 0.065, 1),   # cold rolled steel, dark gray
    "door":  (0.600, 0.620, 0.630, 1),   # galvanized steel, light gray
    "panel": (0.012, 0.014, 0.016, 1),   # steel sheet, black
    "vent":  (0.020, 0.022, 0.025, 1),
    "number": (0.01, 0.01, 0.012, 1),  # black paint, reads on the light door
}

SCREEN_STATES = {                # what the screen shows, and its colour
    "WELCOME":  ((0.05, 0.20, 0.55), "Pick up or drop off"),
    "LOOKUP":   ((0.05, 0.20, 0.55), "Enter phone number"),
    "CONFIRM":  ((0.35, 0.22, 0.02), "Is this the right person?"),
    "QR":       ((0.85, 0.85, 0.85), "Scan this code"),
    "OPEN":     ((0.02, 0.30, 0.22), "Box open - close when done"),
    "REFUSED":  ((0.35, 0.05, 0.03), "That did not work"),
    "OFFLINE":  ((0.30, 0.18, 0.02), "Offline - name not checked"),
}


def mm(v):
    """Millimetres to Blender metres."""
    return v / 1000.0


def _material(name, rgba, emit=None, matte=False):
    mat = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = rgba
    bsdf.inputs["Roughness"].default_value = 0.9 if matte else 0.55
    if "Metallic" in bsdf.inputs:
        # Painted markings are not steel; metallic black reads washed out.
        bsdf.inputs["Metallic"].default_value = 0.0 if (matte or emit is not None) else 0.7
    if emit is not None and "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*emit, 1)
        bsdf.inputs["Emission Strength"].default_value = 2.0
    return mat


def _box(name, size, at, origin_shift=(0, 0, 0), material=None):
    """A box whose origin can sit away from its centre - needed for hinges."""
    sx, sy, sz = (mm(v) / 2 for v in size)
    ox, oy, oz = (mm(v) for v in origin_shift)
    verts = [(x * sx - ox, y * sy - oy, z * sz - oz)
             for x in (-1, 1) for y in (-1, 1) for z in (-1, 1)]
    faces = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1),
             (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    obj.location = tuple(mm(v) for v in at)
    if material:
        obj.data.materials.append(material)
    bpy.data.collections[COLLECTION].objects.link(obj)
    return obj


def _cell(row, col):
    """Front-face rectangle of one grid cell: (left x, bottom z, w, h) in mm."""
    x = -W / 2 + BORDER + col * (CELL_W + GAP)
    z = FOOT + BODY_H - BORDER - row * (CELL_H + GAP) - CELL_H
    return x, z, CELL_W, CELL_H


def _clear():
    old = bpy.data.collections.get(COLLECTION)
    if old:
        for obj in list(old.objects):
            bpy.data.objects.remove(obj, do_unlink=True)
        bpy.data.collections.remove(old)
    bpy.context.scene.collection.children.link(
        bpy.data.collections.new(COLLECTION))


def _shell(mat):
    """Carcass, floor, roof and feet. Doors hang on the front of this."""
    t = 20
    _box("Shell_Back", (W, t, BODY_H), (0, D / 2 - t / 2, FOOT + BODY_H / 2), material=mat)
    _box("Shell_Left", (t, D, BODY_H), (-W / 2 + t / 2, 0, FOOT + BODY_H / 2), material=mat)
    _box("Shell_Right", (t, D, BODY_H), (W / 2 - t / 2, 0, FOOT + BODY_H / 2), material=mat)
    _box("Shell_Top", (W, D, t), (0, 0, FOOT + BODY_H - t / 2), material=mat)
    _box("Shell_Floor", (W, D, t), (0, 0, FOOT + t / 2), material=mat)
    for sx in (-1, 1):
        for sy in (-1, 1):
            _box("Foot", (70, 70, FOOT),
                 (sx * (W / 2 - 90), sy * (D / 2 - 90), FOOT / 2), material=mat)


def _vents(mat):
    """Side vents, top-left and bottom-left as drawn."""
    for i, z in enumerate((FOOT + BODY_H - 220, FOOT + 180)):
        _box(f"Vent_{i}", (18, 260, 150), (-W / 2 + 4, -40, z), material=mat)


def _door(number, x, z, w, h, mat):
    """One door, hinged on its left edge so rotation swings it open."""
    obj = _box(f"Door_{number:02d}", (w, 24, h),
               (x, -D / 2 + 12, z + h / 2),
               origin_shift=(-w / 2, 0, 0), material=mat)
    obj["box_number"] = number
    obj["state"] = "shut"
    _door_number(obj, number, w, h)
    return obj


def _door_number(door, number, w, h):
    """Stencil the box number on the door face, top-left, in dark paint.

    The offset is worked out from the door's own geometry, never from
    `matrix_world`. During build() the depsgraph has not evaluated yet, so
    every object still reports an identity matrix - reading it put the numbers
    at double their intended position, outside the cabinet entirely.

    _box hinges the door on its left edge, so in door space:
      x runs 0 (hinge) .. w,  y is -12 .. 12,  z is -h/2 .. h/2.
    """
    label = bpy.data.objects.new(f"Label_{number:02d}",
                                 bpy.data.curves.new(f"L{number}", "FONT"))
    label.data.body = f"{number:02d}"
    # 100 mm digits. Raised from 72 on 2026-08-07: at 72 the number was hard
    # to read from a metre away at the real cabinet, and illegible in the app,
    # which shows this cabinet at about 300 px wide. hero.py keeps its own
    # copy of this number - see NUMBER_MM there, and change both together.
    label.data.size = mm(100)
    label.data.align_x = "LEFT"
    label.data.align_y = "TOP"
    # Thin flat text reads grey against the door. Give the strokes weight and
    # a little depth so they catch light and stay legible from across a lobby.
    label.data.extrude = mm(1.5)
    label.data.offset = mm(1.2)
    label.data.materials.append(_material("number", PALETTE["number"], matte=True))

    bpy.data.collections[COLLECTION].objects.link(label)
    label.parent = door
    label.matrix_parent_inverse = Matrix.Identity(4)

    # Stand the text up to face the front, 4 mm proud so it never z-fights.
    label.rotation_euler = (math.radians(90), 0, 0)
    label.location = (mm(34), mm(-12 - 4), mm(h / 2 - 30))
    return label


def _panel(mat_panel):
    """Control panel: housing, screen, RFID pad, keypad, status light."""
    r0, r1 = PANEL_ROWS
    x, _, w, _ = _cell(r0, PANEL_COL)
    _, z_bot, _, _ = _cell(r1, PANEL_COL)
    top_z = _cell(r0, PANEL_COL)[1] + CELL_H
    z = z_bot + DOOR10_H + GAP
    h = top_z - z
    _box("Panel_Housing", (w, 30, h), (x + w / 2, -D / 2 + 15, z + h / 2),
         material=mat_panel)

    sh = mm(SCREEN_IN * 25.4) / math.sqrt(1 + (16 / 9) ** 2) * (16 / 9)
    screen_h, screen_w = sh * 1000, sh * 1000 * 9 / 16
    scr = _box("Screen", (screen_w, 6, screen_h),
               (x + w / 2, -D / 2 - 1, z + h - 40 - screen_h / 2),
               material=_material("Mat_Screen", (0, 0, 0, 1),
                                  emit=SCREEN_STATES["WELCOME"][0]))
    scr["state"] = "WELCOME"

    txt = bpy.data.objects.new("Screen_Text",
                               bpy.data.curves.new("ScreenText", "FONT"))
    txt.data.body = SCREEN_STATES["WELCOME"][1]
    txt.data.size = mm(26)
    txt.data.align_x = "CENTER"
    txt.location = (mm(x + w / 2), mm(-D / 2 - 6), mm(z + h - 40 - screen_h / 2))
    txt.rotation_euler = (math.radians(90), 0, 0)
    bpy.data.collections[COLLECTION].objects.link(txt)

    base_z = z + 34
    _box("RFID_Pad", (90, 8, 60), (x + w / 2 - 60, -D / 2 - 1, base_z),
         material=mat_panel)
    _box("Keypad", (110, 8, 62), (x + w / 2 + 70, -D / 2 - 1, base_z),
         material=mat_panel)
    _box("Status_Light", (18, 8, 18), (x + w / 2 + 150, -D / 2 - 1, base_z),
         material=_material("Mat_Status", (0, 0, 0, 1), emit=(0.0, 0.5, 0.2)))


def build():
    """Build the whole cabinet. Safe to run again - it clears the old one."""
    _clear()
    mats = {k: _material(f"Mat_{k.title()}", v) for k, v in PALETTE.items()}
    _shell(mats["frame"])
    _vents(mats["vent"])

    for r, row in enumerate(GRID):
        for c, number in enumerate(row):
            if number is None:
                continue
            x, z, w, h = _cell(r, c)
            if number == 10:                       # short door under the panel
                h = DOOR10_H
            elif c == PANEL_COL and r in PANEL_ROWS:
                continue                           # panel covers this cell
            _door(number, x, z, w, h, mats["door"])

    _panel(mats["panel"])
    doors = len([n for row in GRID for n in row if n is not None])
    print(f"Cabinet built: {doors} doors + 1 control panel.")
    print(f"  {W}x{H}x{D} mm, cell {CELL_W:.0f}x{CELL_H:.0f} mm")
    print("  open_door(n) / close_door(n) / set_screen(state) / reset()")
    return doors


# --- Event API - what the tests actually call ------------------------------

def _find(number):
    obj = bpy.data.objects.get(f"Door_{number:02d}")
    if obj is None:
        raise KeyError(f"No door {number:02d}. Doors are "
                       f"{sorted(n for r in GRID for n in r if n)}.")
    return obj


def open_door(number, angle=OPEN_ANGLE):
    """Swing a door open. This is what 'the server opened box 04' looks like."""
    door = _find(number)
    door.rotation_euler.z = math.radians(-angle)
    door["state"] = "open"
    print(f"box {number:02d} open")
    return door


def close_door(number):
    door = _find(number)
    door.rotation_euler.z = 0
    door["state"] = "shut"
    print(f"box {number:02d} shut")
    return door


def set_screen(state):
    """Change what the cabinet screen shows. State names are in SCREEN_STATES."""
    if state not in SCREEN_STATES:
        raise KeyError(f"Unknown screen state {state!r}. "
                       f"Known: {', '.join(SCREEN_STATES)}")
    colour, caption = SCREEN_STATES[state]
    scr = bpy.data.objects["Screen"]
    bsdf = scr.data.materials[0].node_tree.nodes["Principled BSDF"]
    if "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*colour, 1)
    bpy.data.objects["Screen_Text"].data.body = caption
    scr["state"] = state
    print(f"screen: {state} - {caption}")


def door_states():
    """Every door and whether it is open. The thing a test asserts against."""
    return {n: _find(n)["state"] for row in GRID for n in row if n is not None}


def reset():
    """Every door shut, screen back to Welcome. Run between tests."""
    for row in GRID:
        for n in row:
            if n is not None:
                close_door(n)
    set_screen("WELCOME")
    print("reset")


if __name__ == "__main__":
    build()
