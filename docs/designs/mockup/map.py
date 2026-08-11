"""Project the campus, and the walk to the gate, into one <symbol>.

Nothing here is drawn by eye. `osm.json` is an Overpass response for the box
round the campus; `route.json` is a pedestrian route between two real points,
computed once by a real routing engine. Both are OpenStreetMap data, ODbL.

Why the route is baked in rather than fetched: a published artifact runs under
a CSP that blocks every request, so there is no network at runtime. Re-run this
after refetching to change the route.

    python map.py   ->  rewrites the <symbol id="campus"> block in body.html
"""
import json, math, pathlib, re

HERE = pathlib.Path(__file__).parent
VB_W, VB_H = 306, 150

# The window is sized from the ROUTE, not from the campus. A map on Home is
# there to answer "where do I walk", so the walk is what has to fill it - the
# earlier window was drawn round the whole Overpass box and the route came out
# occupying a fifth of the card's width.
ROUTE = json.loads((HERE / "route.json").read_text(encoding="utf-8"))
lat = [p[0] for p in ROUTE["shape"]]
lon = [p[1] for p in ROUTE["shape"]]

# Leave the route's south end at 75% of the height and its north end at 20%,
# so the walk runs corner to corner with room for the pin's halo at the top and
# for the "you are here" ring above the label bar at the bottom.
span_lat = (max(lat) - min(lat)) / 0.55
N = max(lat) + 0.20 * span_lat
S = N - span_lat
# Square degrees at this latitude, so the plan is not stretched.
span_lon = span_lat * (VB_W / VB_H) / math.cos(math.radians((N + S) / 2))
mid_lon = (min(lon) + max(lon)) / 2
W, E = mid_lon - span_lon / 2, mid_lon + span_lon / 2

x = lambda lo: (lo - W) / (E - W) * VB_W
y = lambda la: (N - la) / (N - S) * VB_H
pt = lambda la, lo: f"{x(lo):.1f},{y(la):.1f}"


def path(geom, close=False):
    d = "M" + " ".join(pt(p["lat"], p["lon"]) for p in geom)
    return d + "Z" if close else d


def visible(geom, pad=40):
    """Drop anything wholly outside the frame - it is bytes for nothing."""
    return any(-pad <= x(p["lon"]) <= VB_W + pad and -pad <= y(p["lat"]) <= VB_H + pad
               for p in geom)


# The plan is drawn in tints of the ink over its own paper, so `ink()` mixes
# into that paper rather than into the app ground - otherwise every weight is
# computed against a value the map never actually shows.
PAPER = "color-mix(in srgb, var(--ink) 8%, var(--ground-2))"
ink = lambda pct: f"color-mix(in srgb, var(--ink) {pct}%, {PAPER})"

# Road class decides weight, the way a printed plan does it: you can tell a
# through road from a footpath without reading a legend.
LAYERS = [
    ("service path footway track",       ink(17), ".6"),
    ("residential unclassified",         ink(23), ".9"),
    ("secondary secondary_link tertiary", ink(31), "1.4"),
    ("trunk trunk_link primary motorway", ink(43), "2.8"),
]

els = json.loads((HERE / "osm.json").read_text(encoding="utf-8"))["elements"]
ways = [e for e in els if e.get("geometry") and visible(e["geometry"])]

out = [f'<symbol id="campus" viewBox="0 0 {VB_W} {VB_H}">',
       f'<rect width="{VB_W}" height="{VB_H}" fill="{PAPER}"/>']

# --- water, then ground cover, then roads, then buildings -----------------
water = [w for w in ways if (w["tags"].get("natural") == "water")]
if water:
    out.append(f'<g stroke="none" fill="{ink(25)}">')
    out += [f'<path d="{path(w["geometry"], True)}"/>' for w in water]
    out.append("</g>")

GROUND = ("pitch", "park", "swimming_pool", "sports_hall", "track")
cover = [w for w in ways if w["tags"].get("leisure") in GROUND]
if cover:
    out.append(f'<g stroke="none" fill="{ink(19)}">')
    out += [f'<path d="{path(w["geometry"], True)}"/>' for w in cover]
    out.append("</g>")

out.append('<g fill="none" stroke-linecap="round" stroke-linejoin="round">')
for classes, colour, width in LAYERS:
    names = set(classes.split())
    group = [w for w in ways if w["tags"].get("highway") in names]
    if not group:
        continue
    out.append(f'<g stroke="{colour}" stroke-width="{width}">')
    out += [f'<path d="{path(w["geometry"])}"/>' for w in group]
    out.append("</g>")
out.append("</g>")

bldg = [w for w in ways if w["tags"].get("building")]
if bldg:
    out.append(f'<g stroke="none" fill="{ink(37)}">')
    out += [f'<path d="{path(w["geometry"], True)}"/>' for w in bldg]
    out.append("</g>")

# --- north ----------------------------------------------------------------
out.append(f'<g stroke="{ink(44)}" stroke-width="1.4" stroke-linecap="round" '
           'fill="none"><path d="M15 118V106M15 106l-3.4 4M15 106l3.4 4"/></g>')
out.append(f'<text x="15" y="129" text-anchor="middle" font-size="7.5" '
           f'font-weight="700" fill="{ink(44)}">N</text>')

# --- the walk -------------------------------------------------------------
# Above every other layer, because it is the answer the card exists to give.
# A casing in the ground colour under the line is what stops it reading as one
# more road when it crosses one.
line = "M" + "L".join(pt(la, lo) for la, lo in ROUTE["shape"])
out.append(f'<path d="{line}" fill="none" stroke="var(--ground-2)" stroke-width="7.5" '
           'stroke-linecap="round" stroke-linejoin="round" opacity=".9"/>')
out.append(f'<path d="{line}" fill="none" stroke="var(--accent)" stroke-width="3.4" '
           'stroke-linecap="round" stroke-linejoin="round"/>')

# A dot and a pin, because the two ends of a walk are not the same kind of
# thing. Two coloured dots say "these are both places"; a dot and a pin say
# "you are here, that is where you are going" with no legend and no copy.
sy, sx = ROUTE["shape"][0]
out.append(f'<circle cx="{x(sx):.1f}" cy="{y(sy):.1f}" r="5.6" '
           f'fill="{ink(62)}" stroke="{PAPER}" stroke-width="2.8"/>')

gy, gx = ROUTE["to"]
gxp, gyp = x(gx), y(gy)
out.append('<!-- the side gate: OSM node 12093474313, barrier=gate, '
           'open 06:00-23:00, 7 m from the place Google labels Cong phu -->')
# A footprint under the tip, not a second marker beside it. At r=14 the halo
# read as a disc the pin happened to be standing next to.
out.append(f'<ellipse cx="{gxp:.1f}" cy="{gyp:.1f}" rx="7" ry="3.4" '
           'fill="color-mix(in srgb, var(--accent) 22%, transparent)"/>')
# The teardrop's TIP sits on the gate and the body stands above it, the way a
# pin pushed into a board does. Centring it would put the gate half a pin's
# height south of where it actually is.
out.append(f'<g transform="translate({gxp:.1f} {gyp:.1f})">'
           '<path d="M0 0C0 0-8.4-9.2-8.4-14.2A8.4 8.4 0 0 1 8.4-14.2C8.4-9.2 0 0 0 0Z" '
           f'fill="var(--accent)" stroke="{PAPER}" stroke-width="2"/>'
           f'<circle cy="-14.2" r="3.1" fill="{PAPER}"/></g>')
out.append("</symbol>")

svg = "\n".join(out)
(HERE / "campus-symbol.svg").write_text(svg, encoding="utf-8")

body = HERE / "body.html"
src = body.read_text(encoding="utf-8")
src, n = re.subn(r'<symbol id="campus".*?</symbol>', svg, src, count=1, flags=re.S)
assert n == 1, "the campus symbol was not found in body.html"
body.write_text(src, encoding="utf-8")

print(f"window  {S:.6f}..{N:.6f} N   {W:.6f}..{E:.6f} E")
print(f"gate    {x(gx):.1f},{y(gy):.1f}     you {x(sx):.1f},{y(sy):.1f}")
print(f"ways    {len(ways)} drawn of {len(els)}   route {ROUTE['metres']} m, "
      f"{ROUTE['seconds']} s -> {round(ROUTE['seconds'] / 60)} min")
print(f"symbol  {len(svg) // 1024} KB")
