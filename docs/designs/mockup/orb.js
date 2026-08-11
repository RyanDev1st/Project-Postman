/* ======================================================================
   The thinking orb — ported from the real component, third attempt.

   Attempt 1 was three blurred blobs on unequal periods. Not the component
   at all; a lava lamp.

   Attempt 2 read the parameters off the bundle correctly and then invented
   the geometry: five flat concentric circles with a faked depth value. It
   had the right numbers and the wrong shape, which is the worst kind of
   wrong, because the numbers make it look researched.

   This is the actual renderer, `es` in the bundle, transcribed. What the
   guess got wrong:

   **The bands are great circles on a SPHERE.** Every point is normalised -
   `p / |p| * radius` - so the bands bulge and gather toward the rim exactly
   the way lines of longitude do. Flat circles cannot do that, and the
   bulge is most of what makes it read as an object rather than a target.

   **There is a real 3D basis and a real projection.** Three orthogonal axes
   built per frame, then a yaw/pitch projector. The faked `z` in attempt 2
   drove the shading but not the positions, so the shading described a
   sphere that the geometry did not.

   **`spin: 0`.** The ring state does not rotate. All of the motion is the
   wobble, which is why the state is called `breathing`. Attempt 2 span it.

   **The counts are not the base numbers.** `lanes: 5, segs: 88` are scaled
   by `sqrt(count)` at 0.25, so 3 and 44 - and then the band count is
   `round(lanes * bandMul)` = `round(3 * 3.627)` = **11 bands of 44**, not 5
   rings of 88. Roughly the same dot budget arranged completely differently.

   **The dots are small.** `Vt(size, .6)` = `(size/300) ** .6`, so at 132 CSS
   pixels on a 2x screen the radius runs about 0.9 to 2.5 device pixels.
   Attempt 2 drew them several times that.

   Source: orbs.jakubantalik.com, ThinkingOrb-DUWt7sqA.js. "Thinking…" is
   the `breathing` state, which maps to the `ring` structure.
   ====================================================================== */

/* ---- which structure, and why it is not the one called "Thinking…" -----

   The component's own map is `breathing -> "ring"`, and `breathing` is
   labelled "Thinking…". So `ring` was the literal answer, and it was
   transcribed here faithfully. Then it was rendered and measured, and it is
   a thin hoop of dashes - nothing like the spheres in the reference.

   That is not a porting bug. Working the projection through by hand: the
   basis is built at pitch `-c` when `faceOn` is set, and the projector is
   built at pitch `+c`. The two cancel exactly, so every band collapses onto
   the screen circle `(cos θ, sin θ) · r / sqrt(1 + I²)`. With eleven bands
   at an offset of 0.075 the radii only span `0.936r` to `r` - a 6% spread.
   Eleven near-identical circles on top of each other IS a hoop. The
   structure is called `ring` because it is one.

   The spheres are `globe`, the structure behind "Searching…" - latitude
   rings whose dot count follows `|cos(latitude)|`, so they crowd at the
   equator and converge at the poles, spun on a yaw with a highlight
   sweeping around it.

   `globe` is used here. It is what the reference shows, and it is also the
   better fit for the moment: this screen is captioned "Checking your
   parcels", which is a search, not a thought.

   base    globe: { latRings: 17, lonDensity: 44, rBase: .6, rDepth: 1.7,
                    rBoost: 1, inkFar: .62, inkSpan: .54, rsPow: .6, rMin: .3 }
   tuning  globe[64]: { speed: 2.015, count: .42, size: 1.15,
                        extra: { scanMul: 4.08, dimBase: .45 } }

   `mp()` scales the [latRings, lonDensity] pair by sqrt(count); `vp()`
   scales every radius by size. Applied here, since neither input changes. */
const P = {
  latRings: Math.max(2, Math.round(17 * Math.sqrt(0.42))),    // 11
  lonDensity: Math.max(2, Math.round(44 * Math.sqrt(0.42))),  // 29
  rBase: 0.6 * 1.15,
  rDepth: 1.7 * 1.15,
  rBoost: 1 * 1.15,
  inkFar: 0.62,
  inkSpan: 0.54,
  rsPow: 0.6,
  rMin: 0.3,
  scanMul: 4.08,
  dimBase: 0.45,
  speed: 2.015,
};

/** `Bt` — a yaw/pitch projector. Returns [screenX, screenY, depth]. */
function projector(yaw, pitch, cx, cy, scale) {
  const sp = Math.sin(pitch), cp = Math.cos(pitch);
  const sy = Math.sin(yaw), cy2 = Math.cos(yaw);
  return (px, py, pz) => {
    const a = px * cy2 + pz * sy;
    const b = -px * sy + pz * cy2;
    const yy = py * cp - b * sp;
    const zz = py * sp + b * cp;
    return [cx + a * scale, cy - yy * scale, zz];
  };
}

/** `Vt` — dot radius scale for a given canvas size. */
const radiusScale = (size, pow) => (size / 300) ** pow;

/**
 * `Ct` — the painter.
 *
 * Depth-sorted back to front, and each dot's grey comes from its own depth.
 * `invert` flips the ramp for a dark ground, which is what the component's
 * theme flag does.
 */
function paint(ctx, dots, invert, rMin) {
  dots.sort((a, b) => a.z - b.z);
  for (const d of dots) {
    const alpha = d.a ?? 1;
    if (alpha < 0.02) continue;
    const wclamped = Math.min(1, Math.max(0, d.white));
    const level = Math.round((invert ? 1 - wclamped : wclamped) * 255);
    ctx.fillStyle = `rgba(${level},${level},${level},${alpha})`;
    ctx.beginPath();
    ctx.arc(d.x, d.y, Math.max(rMin, d.r), 0, Math.PI * 2);
    ctx.fill();
  }
}

/** Signed angular difference, `Yd` in the bundle. */
const angleDelta = (a, b) => Math.atan2(Math.sin(a - b), Math.cos(a - b));

/** `bd` — the globe renderer, transcribed. */
function drawGlobe(ctx, size, t, invert) {
  const cx = size / 2, cy = size / 2, R = (size / 2) * 0.82;

  // The pitch breathes very slightly, so the poles drift rather than sit.
  const pitch = 0.4 + 0.06 * Math.sin(t * 0.35);
  const project = projector(t * 0.5, pitch, cx, cy, R);
  const scan = t * (0.5 + (1.7 - 0.5) * P.scanMul);
  const rs = radiusScale(size, P.rsPow);
  const dots = [];

  for (let i = 0; i <= P.latRings; i++) {
    const lat = -Math.PI / 2 + (i / P.latRings) * Math.PI;
    const cosLat = Math.cos(lat), sinLat = Math.sin(lat);
    // The whole character of the thing: the number of dots on a latitude
    // follows |cos(lat)|, so the equator is crowded and the poles converge
    // to one. Even spacing would give a barrel, not a globe.
    const n = Math.max(1, Math.round(Math.abs(cosLat) * P.lonDensity));

    for (let j = 0; j < n; j++) {
      const lon = (j / n) * 2 * Math.PI;
      const [sx, sy, sz] = project(cosLat * Math.cos(lon), sinLat,
                                   cosLat * Math.sin(lon));
      const depth = (sz + 1) / 2;

      // A highlight sweeping round the equator, only on the near face.
      const d = angleDelta(lon + t * 0.5, scan);
      const lit = Math.exp(-(d * d) / 0.18) * Math.max(0, sz);

      dots.push({
        x: sx, y: sy, z: sz,
        r: (P.rBase + P.rDepth * depth + P.rBoost * lit) * rs,
        white: P.inkFar - P.inkSpan * depth,
        a: P.dimBase + (1 - P.dimBase) * Math.min(1, lit),
      });
    }
  }
  paint(ctx, dots, invert, P.rMin);
}

/* ---- mount ----------------------------------------------------------- */

const onDark = () =>
  document.documentElement.getAttribute("data-theme") !== "light";

const canvases = [...document.querySelectorAll("canvas.orb")];
const reduced = matchMedia("(prefers-reduced-motion: reduce)");

canvases.forEach((c) => {
  const css = Number(c.dataset.size || 132);
  const dpr = Math.min(2, devicePixelRatio || 1);
  c.width = Math.round(css * dpr);
  c.height = Math.round(css * dpr);
  c.style.width = `${css}px`;
  c.style.height = `${css}px`;
  c.__ctx = c.getContext("2d");
  c.__size = c.width;
});

let started = 0;
function frame(now) {
  if (!started) started = now;
  // Held still for reduced motion: one pose, drawn once per theme change,
  // rather than the same animation slowed down.
  const t = reduced.matches ? 0 : ((now - started) / 1000) * P.speed;
  const invert = onDark();
  for (const c of canvases) {
    c.__ctx.clearRect(0, 0, c.__size, c.__size);
    drawGlobe(c.__ctx, c.__size, t, invert);
  }
  requestAnimationFrame(frame);
}
if (canvases.length) requestAnimationFrame(frame);
