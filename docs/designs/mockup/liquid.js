/* ======================================================================
   Liquid Glass — ported from rdev/liquid-glass-react.

   Four goes at this. The first three failed in instructive ways:

   1. **Glassmorphism.** Blur, white tint, a 1px line. A painting of glass.
   2. **Snell's law refraction.** Physically correct: SDF, convex bezel,
      n = 1.5, per-pixel ray deviation. It produced ~9 px of bend, which is
      what 11 mm of plate glass really does, and looks nothing like Apple's.
   3. **Ramp maps at 150 px.** Bent hard enough, but bent the *whole panel*.

   rdev's version is architecturally different from all three, and the
   differences are the effect:

   **The centre is not displaced at all.** The filter builds an edge mask
   out of the displacement map's own luminance, runs the aberration only
   inside it, and composites an *undisplaced* copy of the backdrop into the
   middle. Content behind the middle of a panel is pin sharp; only the rim
   warps. Every earlier attempt here smeared the whole panel, which is why
   they read as "a wobbly rectangle" and not as glass.

   **The map is a shader, not a gradient.** A rounded-rect SDF run through
   two smoothsteps, evaluated per pixel, giving displacement that ramps
   smoothly to zero. `fragmentShaders.liquidGlass`, kept verbatim.

   **The warp is a separate layer.** `filter:` on an absolutely-positioned
   span behind the content, not `backdrop-filter` on the element. So the
   text stays sharp while the backdrop moves, and it works in Safari.

   **Two border layers**, screen and overlay blended, mask-composited to a
   1.5 px ring, with a light direction that follows the pointer.

   Source: github.com/rdev/liquid-glass-react (MIT), whose shader in turn
   comes from shuding/liquid-glass. Ported from React to plain DOM because
   the artifact cannot fetch or bundle anything at run time.
   ====================================================================== */

const GLASS = {
  // --- how far the rim bends the backdrop ------------------------------
  //
  // rdev exports a single `displacementScale` of 70. The map is normalised
  // to +/-0.5 around neutral, so `feDisplacementMap` moves a pixel by
  // scale * 0.5 - which is **35 px on every element, whatever its size**.
  //
  // Their demo is one large card, where 35 px is a modest rim. Across a real
  // interface it is not: measured against these panels it came to 80% of the
  // nav bar's height and 117% of a 30 px round button. The whole backdrop
  // was being dragged, which is what "too extreme" looks like.
  //
  // Apple's bend lives in the bezel, so it scales with the object. A fraction
  // of the short side, capped in absolute pixels so a big panel does not get
  // a wide smear:
  bendRatio: 0.30,    // of the shorter side
  bendMax: 18,        // px, whatever the element's size
  bendMin: 5,

  aberrationIntensity: 2,
  saturation: 200,

  // --- how much light the pane passes on --------------------------------
  //
  // This was one number, 1.55, used in both schemes. It was chosen for dark,
  // where the backdrop is near-black and a pane with no gain is a hole cut
  // through the interface rather than an object lying on it.
  //
  // In light it does the opposite of help. The ground is #D3DBE3, so 1.55
  // takes 211 to 327 and every channel clips at 255. Measured on the nav in
  // light mode: average rgb(241,243,245) - a white lozenge with the card
  // edges behind it wiped out. Glass you cannot see through is not glass.
  //
  // A light backdrop does not need gain. It needs to be *tinted* toward
  // white, which is what the translucent fill in glass.css does, while the
  // filter's whole job is to keep the hue and soften the detail.
  liftDark: 1.5,
  liftLight: 1.02,

  blurAmount: 0.0625,   // x32 in the backdrop, so ~2 px on top of the base 4
  elasticity: 0.12,     // how far the sheen leans toward the pointer
};

const isDark = () =>
  document.documentElement.getAttribute("data-theme") !== "light";

/** The `feDisplacementMap` scale that gives an element a bezel-sized bend. */
function bendFor(w, h) {
  const px = Math.min(GLASS.bendMax,
             Math.max(GLASS.bendMin, Math.min(w, h) * GLASS.bendRatio));
  return px * 2;      // the map only ever reaches half of `scale`
}

const NS = "http://www.w3.org/2000/svg";
const XLINK = "http://www.w3.org/1999/xlink";
const FIREFOX = navigator.userAgent.toLowerCase().includes("firefox");

let uid = 0;
const mapCache = new Map();

/* ---------- the shader ------------------------------------------------ */
// Verbatim from rdev/liquid-glass-react src/shader-utils.ts.

function smoothStep(a, b, t) {
  t = Math.max(0, Math.min(1, (t - a) / (b - a)));
  return t * t * (3 - 2 * t);
}

function roundedRectSDF(x, y, width, height, radius) {
  const qx = Math.abs(x) - width + radius;
  const qy = Math.abs(y) - height + radius;
  return Math.min(Math.max(qx, qy), 0) +
         Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) - radius;
}

/** uv in 0..1 -> where to sample from. The whole effect is these five lines. */
function liquidGlassFragment(uv) {
  const ix = uv.x - 0.5;
  const iy = uv.y - 0.5;
  const distanceToEdge = roundedRectSDF(ix, iy, 0.3, 0.2, 0.6);
  const displacement = smoothStep(0.8, 0, distanceToEdge - 0.15);
  const scaled = smoothStep(0, 1, displacement);
  return { x: ix * scaled + 0.5, y: iy * scaled + 0.5 };
}

/**
 * Render the shader to a displacement map.
 *
 * Note the blue channel gets the *Y* displacement, not the usual zero: the
 * filter below selects `yChannelSelector="B"`, because the edge mask is built
 * from the map's own luminance and a flat blue channel would wash it out.
 */
function shaderMap(width, height) {
  const key = `${width}x${height}`;
  const hit = mapCache.get(key);
  if (hit) return hit;

  const w = Math.max(1, Math.round(width));
  const h = Math.max(1, Math.round(height));
  const canvas = document.createElement("canvas");
  canvas.width = w; canvas.height = h;
  const ctx = canvas.getContext("2d");

  const raw = new Float32Array(w * h * 2);
  let maxScale = 0;

  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const pos = liquidGlassFragment({ x: x / w, y: y / h });
      const dx = pos.x * w - x;
      const dy = pos.y * h - y;
      maxScale = Math.max(maxScale, Math.abs(dx), Math.abs(dy));
      const i = (y * w + x) * 2;
      raw[i] = dx; raw[i + 1] = dy;
    }
  }
  maxScale = Math.max(maxScale, 1);

  const img = ctx.createImageData(w, h);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const i = (y * w + x) * 2;
      // Fade the outermost two pixels, or the map ends in a hard step and
      // the rim gets a visible seam.
      const edge = Math.min(1, Math.min(x, y, w - x - 1, h - y - 1) / 2);
      const r = (raw[i] * edge) / maxScale + 0.5;
      const g = (raw[i + 1] * edge) / maxScale + 0.5;
      const p = (y * w + x) * 4;
      img.data[p] = Math.max(0, Math.min(255, r * 255));
      img.data[p + 1] = Math.max(0, Math.min(255, g * 255));
      img.data[p + 2] = Math.max(0, Math.min(255, g * 255));
      img.data[p + 3] = 255;
    }
  }
  ctx.putImageData(img, 0, 0);

  const url = canvas.toDataURL();
  mapCache.set(key, url);
  return url;
}

/* ---------- the filter ------------------------------------------------ */
/**
 * Ported from rdev's `GlassFilter`. The important part is the last third:
 * an edge mask cut from the map's own luminance, the aberration composited
 * `in` that mask, and the undisplaced backdrop composited into the inverse.
 * That is what keeps the middle of the panel sharp.
 */
function buildFilter(id, mapUrl, scale, aberration) {
  const svg = document.createElementNS(NS, "svg");
  svg.setAttribute("aria-hidden", "true");
  svg.style.cssText = "position:absolute;width:0;height:0;pointer-events:none";

  const el = (name, attrs, parent) => {
    const n = document.createElementNS(NS, name);
    for (const k in attrs) n.setAttribute(k, attrs[k]);
    parent.append(n);
    return n;
  };

  const defs = el("defs", {}, svg);
  const filter = el("filter", {
    id,
    x: "-35%", y: "-35%", width: "170%", height: "170%",
    "color-interpolation-filters": "sRGB",
  }, defs);

  const img = el("feImage", {
    x: "0", y: "0", width: "100%", height: "100%",
    result: "DISPLACEMENT_MAP",
    preserveAspectRatio: "xMidYMid slice",
  }, filter);
  img.setAttribute("href", mapUrl);
  img.setAttributeNS(XLINK, "xlink:href", mapUrl);

  // The map's luminance IS the edge mask: it is neutral grey in the middle
  // and swings away from grey at the rim.
  el("feColorMatrix", {
    in: "DISPLACEMENT_MAP", type: "matrix", result: "EDGE_INTENSITY",
    values: "0.3 0.3 0.3 0 0  0.3 0.3 0.3 0 0  0.3 0.3 0.3 0 0  0 0 0 1 0",
  }, filter);
  const tr = el("feComponentTransfer", { in: "EDGE_INTENSITY", result: "EDGE_MASK" }, filter);
  el("feFuncA", { type: "discrete", tableValues: `0 ${aberration * 0.05} 1` }, tr);

  // The clean, undisplaced copy that will fill the middle.
  el("feOffset", { in: "SourceGraphic", dx: "0", dy: "0", result: "CENTER_ORIGINAL" }, filter);

  const channels = [
    ["RED", 1, "1 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 1 0"],
    ["GREEN", 1 - aberration * 0.05, "0 0 0 0 0  0 1 0 0 0  0 0 0 0 0  0 0 0 1 0"],
    ["BLUE", 1 - aberration * 0.1, "0 0 0 0 0  0 0 0 0 0  0 0 1 0 0  0 0 0 1 0"],
  ];
  for (const [name, mult, matrix] of channels) {
    el("feDisplacementMap", {
      in: "SourceGraphic", in2: "DISPLACEMENT_MAP",
      scale: String(scale * mult),
      xChannelSelector: "R", yChannelSelector: "B",
      result: `${name}_DISPLACED`,
    }, filter);
    el("feColorMatrix", {
      in: `${name}_DISPLACED`, type: "matrix", values: matrix,
      result: `${name}_CHANNEL`,
    }, filter);
  }

  el("feBlend", { in: "GREEN_CHANNEL", in2: "BLUE_CHANNEL", mode: "screen", result: "GB" }, filter);
  el("feBlend", { in: "RED_CHANNEL", in2: "GB", mode: "screen", result: "RGB" }, filter);
  el("feGaussianBlur", {
    in: "RGB", stdDeviation: String(Math.max(0.1, 0.5 - aberration * 0.1)),
    result: "ABERRATED",
  }, filter);

  // Aberration only at the rim...
  el("feComposite", { in: "ABERRATED", in2: "EDGE_MASK", operator: "in", result: "EDGE_ABERRATION" }, filter);
  // ...and the untouched backdrop everywhere else.
  const inv = el("feComponentTransfer", { in: "EDGE_MASK", result: "INVERTED_MASK" }, filter);
  el("feFuncA", { type: "table", tableValues: "1 0" }, inv);
  el("feComposite", { in: "CENTER_ORIGINAL", in2: "INVERTED_MASK", operator: "in", result: "CENTER_CLEAN" }, filter);
  el("feComposite", { in: "EDGE_ABERRATION", in2: "CENTER_CLEAN", operator: "over" }, filter);

  document.body.append(svg);
  return svg;
}

/* ---------- applying it ----------------------------------------------- */

const BORDER_MASK =
  "linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0)";

/**
 * The rim.
 *
 * rdev stacks two of these, each 1.5 px thick, each carrying a hard
 * `inset 0 0 0 0.5px rgba(255,255,255,.5)` ring, with the second at full
 * opacity in `overlay` blend. Over the bright photographs in his demo that
 * reads as glass. On a dark interface, overlay-blending white produces a
 * thick chalky outline - measured, and it was the first thing wrong with
 * this at a glance.
 *
 * Apple's edge is not a border. It is a **specular highlight**: bright along
 * the arc the light catches, fading to nothing on the opposite side, and
 * thin enough to read as a lit edge rather than a drawn line. So:
 *
 *   - one ring, not two
 *   - one device pixel, not one and a half CSS pixels
 *   - no hard inset ring at all - that was the chalk
 *   - a gradient that actually goes to zero at both ends, so three quarters
 *     of the perimeter has no line on it
 *
 * The lit arc leans toward the pointer, which is what makes it feel liquid
 * rather than printed.
 */
function ring(el) {
  const span = document.createElement("span");
  span.className = "lg-ring";
  span.style.cssText = `
    position:absolute; inset:0; border-radius:inherit; pointer-events:none;
    padding:1px;
    -webkit-mask:${BORDER_MASK}; mask:${BORDER_MASK};
    -webkit-mask-composite:xor; mask-composite:exclude;
  `;
  el.append(span);
  return span;
}

/** Where the light is, and how the rim catches it. */
function sheen(span, ox, oy) {
  // 135deg puts the bright arc across the top-left, which is where every
  // other light in this design comes from - the cabinet render included.
  const angle = 135 + ox * 0.9;
  span.style.background = `linear-gradient(${angle}deg,
    rgba(255,255,255,0) 12%,
    rgba(255,255,255,.34) 34%,
    rgba(255,255,255,.10) 52%,
    rgba(255,255,255,0) 68%)`;
}

const tracked = [];

function apply(el) {
  const w = el.offsetWidth, h = el.offsetHeight;
  if (!w || !h) return;
  if (el.dataset.lgAt === `${w}x${h}`) return;
  el.dataset.lgAt = `${w}x${h}`;

  let warp = el.querySelector(":scope > .lg-warp");
  if (!warp) {
    warp = document.createElement("span");
    warp.className = "lg-warp";
    warp.style.cssText =
      "position:absolute; inset:0; border-radius:inherit; pointer-events:none;";
    el.prepend(warp);

    const rings = [ring(el)];
    tracked.push({ el, rings });
    rings.forEach((r) => sheen(r, 0, 0));
  }

  const id = `lg-${++uid}`;
  buildFilter(id, shaderMap(w, h), bendFor(w, h), GLASS.aberrationIntensity);

  // The filter goes on the warp layer, not on the element, so content stays
  // sharp. Firefox does not run SVG filters here and gets the blur alone.
  warp.style.filter = FIREFOX ? "none" : `url(#${id})`;

  paintBackdrop(warp);
}

/**
 * The backdrop chain, in the order that survives clipping.
 *
 * It used to read `brightness() saturate() blur()`, on the argument that the
 * saturation should work on lifted values rather than crushed ones. That is
 * true going down and false going up: `brightness` above 1 pushes channels
 * past 255, they clamp, and a clamped pixel has **no chroma left for
 * `saturate` to find**. On a light ground every channel clipped, so the
 * saturation stage was operating on flat white and doing nothing at all -
 * which is why light mode had no colour in it and read as a plain white
 * lozenge rather than a pane.
 *
 * Saturate first. The chroma is taken while the values are still intact, and
 * whatever brightness does afterwards, the hue of what is behind the glass
 * has already been carried through.
 */
function paintBackdrop(warp) {
  const lift = isDark() ? GLASS.liftDark : GLASS.liftLight;
  const backdrop = `saturate(${GLASS.saturation}%) brightness(${lift}) ` +
                   `blur(${4 + GLASS.blurAmount * 32}px)`;
  warp.style.backdropFilter = backdrop;
  warp.style.setProperty("-webkit-backdrop-filter", backdrop);
}

// The lift differs per scheme, so it has to be repainted when the scheme
// changes. `apply()` early-returns once an element is sized, so it would
// never revisit this on its own.
new MutationObserver(() => {
  document.querySelectorAll(".lg > .lg-warp").forEach(paintBackdrop);
}).observe(document.documentElement, {
  attributes: true, attributeFilter: ["data-theme"],
});

function applyAll() { document.querySelectorAll(".lg").forEach(apply); }

document.documentElement.classList.add("lg-real");

applyAll();
if (document.fonts && document.fonts.ready) document.fonts.ready.then(applyAll);
addEventListener("load", applyAll);

let redo;
const watch = new ResizeObserver(() => {
  clearTimeout(redo);
  redo = setTimeout(applyAll, 140);
});
document.querySelectorAll(".lg").forEach((el) => watch.observe(el));

// One pointer listener for the page rather than one per panel: there are
// twenty-seven of these, and each would otherwise do its own geometry read
// on every mouse move.
let queued = false;
addEventListener("pointermove", (e) => {
  if (queued) return;
  queued = true;
  requestAnimationFrame(() => {
    queued = false;
    for (const { el, rings } of tracked) {
      const r = el.getBoundingClientRect();
      if (!r.width) continue;
      const ox = ((e.clientX - (r.left + r.width / 2)) / r.width) * 100;
      const oy = ((e.clientY - (r.top + r.height / 2)) / r.height) * 100;
      const near = Math.max(-60, Math.min(60, ox));
      const nearY = Math.max(-60, Math.min(60, oy));
      rings.forEach((ring) => sheen(ring, near * GLASS.elasticity * 8,
                                         nearY * GLASS.elasticity * 8));
    }
  });
}, { passive: true });
