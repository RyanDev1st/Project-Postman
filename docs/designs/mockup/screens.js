// Door corners, projected through the render camera by hero.py. Normalised
// 0..1 with the origin at the top left, so they hold at any display size.
const DOORS = __DOORS__;
const VB_W = 2100, VB_H = 2100;   // the overlay's viewBox, matching the render

const FREE = [2, 5, 11, 14, 17, 20];

/* More than one box can be yours, and until now the app could not say so.
   ------------------------------------------------------------------------
   `YOUR_BOX = 4` was a single number, so the cabinet tab could light exactly
   one door and the heading could only ever read "Box 04". Two parcels at the
   same cabinet is not an edge case - it is a Tuesday, and it is the one
   moment where the receiver genuinely does not know what to do, because the
   flow's answer to "which box opens when I scan?" is silent about it.

   So the doors are a list, each carrying when it was dropped and how long is
   left, and the picker under the render is the answer: you choose the box,
   then you scan. Both the lit doors and the chips are built from THIS array,
   so the two can never disagree with each other. */
const ALL_MINE = [
  { n: 4, at: "08:14 today",      left: "6h left",  pct: 0.12, soon: true },
  { n: 7, at: "21:40 yesterday",  left: "31h left", pct: 0.65, soon: false },
];
let mine = ALL_MINE;

// How much of the frame the chosen door should fill once the screen has
// pushed into it.
//
// It was 0.58, and on screen that is too far: the door filled the panel, its
// neighbours were cropped away, and what was left was a large amber square
// with no way to tell where on the cabinet it sat. The point of the move is
// to teach *which* door, which needs the doors around it still in frame.
const ZOOM_COVERAGE = 0.36;

const holder = document.getElementById("holder");
const glows = document.getElementById("glows");
const faces = document.getElementById("faces");
const hits = document.getElementById("hits");
const overlayDefs = document.querySelector("#overlay defs");
const cabinfo = document.getElementById("cabinfo");
const hero = document.getElementById("hero");
const novac = document.getElementById("novac");
const action = document.getElementById("action");
const title = document.getElementById("title");
const sub = document.getElementById("sub");
const go = document.getElementById("go");
const gotext = document.getElementById("gotext");
const quiet = document.getElementById("quiet");

const pad = (n) => String(n).padStart(2, "0");

const NS = "http://www.w3.org/2000/svg";
const cssVar = (n) =>
  getComputedStyle(document.documentElement).getPropertyValue(n).trim();

const corners = (n) => DOORS[String(n)];
const pointsOf = (n) =>
  corners(n).map(([x, y]) => `${x * VB_W},${y * VB_H}`).join(" ");

/** Centre and size of a door, in fractions of the frame. */
function boxOf(n) {
  const c = corners(n);
  const xs = c.map((p) => p[0]), ys = c.map((p) => p[1]);
  const x0 = Math.min(...xs), x1 = Math.max(...xs);
  const y0 = Math.min(...ys), y1 = Math.max(...ys);
  return { cx: (x0 + x1) / 2, cy: (y0 + y1) / 2, w: x1 - x0, h: y1 - y0 };
}

const el = (name, attrs) => {
  const node = document.createElementNS(NS, name);
  for (const k in attrs) node.setAttribute(k, attrs[k]);
  return node;
};

/**
 * Tint one door, and let its own stencilled number show through.
 *
 * ## What this replaced, and why it was wrong
 *
 * The door used to be filled with flat amber at 82% and the number DRAWN BACK
 * ON TOP - a mono glyph at 42% of the door's short side, centred on the
 * projected box. It was wrong twice over, and Ryan caught both at a glance:
 *
 *   - it is not the real number. The stencil on the render is a specific size
 *     in a specific place with the cabinet's own lighting on it. The redrawn
 *     one is bigger, flatter, in a different face, and it sits dead-centre
 *     where the real one does not. The eye reads the mismatch instantly even
 *     when it cannot name it;
 *   - it is a lie about the object. The whole argument for building this app
 *     around a render is that the receiver is looking at the thing they will
 *     walk up to. Painting over the thing and re-lettering it gives that away
 *     for a highlight.
 *
 * ## What it does instead
 *
 * `mix-blend-mode: color` takes hue and saturation from the fill and
 * **luminance from the backdrop**. The door goes amber; every value inside it
 * - the stencil, the shadow in the door gap, the specular along the top edge
 * - is exactly the value the render already had. Nothing is covered, so
 * nothing has to be redrawn.
 *
 * Two things make that work and both are load-bearing:
 *
 *   - the tint is a SEPARATE `<svg id="tint">` layer carrying the blend mode,
 *     and `.holder` is `isolation: isolate`. A blend mode composites against
 *     the backdrop of its parent stacking context, so the layer has to sit
 *     above the `<img>` inside a group that contains it;
 *   - the bloom is MASKED OUT of the door. A blurred amber blob centred on the
 *     door is part of the backdrop the tint then blends against, and it would
 *     wash out exactly the values the blend exists to preserve. Knocked out,
 *     it becomes what it was always supposed to be: a halo around a lit door.
 */
const masks = [];

function lightDoor(n, colour) {
  const p = pointsOf(n);

  const mid = `halo-${n}`;
  const mask = el("mask", {
    id: mid, maskUnits: "userSpaceOnUse", x: 0, y: 0, width: VB_W, height: VB_H,
  });
  mask.append(el("rect", { x: 0, y: 0, width: VB_W, height: VB_H, fill: "#fff" }));
  mask.append(el("polygon", { points: p, fill: "#000" }));
  overlayDefs.append(mask);
  masks.push(mask);

  glows.append(el("polygon", {
    points: p, fill: colour, "fill-opacity": 0.95,
    filter: "url(#bloom)", mask: `url(#${mid})`,
  }));
  glows.append(el("polygon", {
    points: p, fill: "none", stroke: colour, "stroke-width": 7,
    "stroke-linejoin": "round", filter: "url(#bloom-soft)",
  }));

  faces.append(el("polygon", { points: p, fill: colour }));

  // An invisible target on the door itself. Tapping the thing you are looking
  // at is the first instinct, and a chip below the picture is the second.
  const hit = el("polygon", { points: p, fill: "transparent" });
  hit.style.cursor = "pointer";
  hit.addEventListener("click", () => toggleDoor(n));
  hits.append(hit);
}

function markFree(n) {
  const p = pointsOf(n);
  const free = cssVar("--free");
  faces.append(el("polygon", { points: p, fill: free }));
  glows.append(el("polygon", {
    points: p, fill: "none", stroke: free, "stroke-width": 5,
    "stroke-opacity": 0.95, "stroke-linejoin": "round",
  }));
}

const clearGlows = () => {
  glows.textContent = "";
  faces.textContent = "";
  hits.textContent = "";
  masks.splice(0).forEach((m) => m.remove());
};

// Same curves as the loader film. Nothing linear, and nothing overshoots -
// see the note at the top of motion.js for why not.
const ARRIVE = "power3.out";
const TRAVEL = "power2.inOut";

let mode = "parcel";
let slow = false;
let shot = null;      // the one timeline; there is never a second

/**
 * Push the frame into one door.
 *
 * The render is an orthographic elevation, so a door is the same rectangle
 * wherever it sits and this is only a scale and a slide - no perspective to
 * correct and no skew on the number. Scaling about the door's own centre pins
 * it in place; the translate that follows carries that point to the middle of
 * the frame.
 */
function zoomFor(n) {
  const b = boxOf(n);
  const scale = ZOOM_COVERAGE / b.w;

  // Keep the frame inside the picture.
  //
  // Centring on a door in the top row pans the image down far enough that
  // the empty space above the cabinet comes into frame, and the shot ends
  // with a third of it blank. After scaling by `k` the visible window is
  // 1/k of the image wide, so the centre can only travel to within half of
  // that of each edge. Clamping to that range keeps the cabinet filling the
  // frame; the door lands slightly off-centre instead, which is fine - it is
  // still the only lit thing on screen.
  const half = 0.5 / scale;
  const cx = Math.min(Math.max(b.cx, half), 1 - half);
  const cy = Math.min(Math.max(b.cy, half), 1 - half);

  // Origin stays at the centre and the pan is solved for, rather than moving
  // the origin onto the door. It has to, now that the push is reversible and
  // repeatable: with a per-door origin, going wide again or switching to
  // another door means interpolating `transform-origin` as well, and a moving
  // origin under a moving scale sends the image on a curve nobody asked for.
  // Fixed origin makes every state a plain (scale, x, y) that any two of can
  // be tweened between.
  //
  // With `transform: translate(t) scale(k)` about the centre, an image point p
  // lands at `0.5 + k(p - 0.5) + t`, so `t = -k(c - 0.5)` puts c in the middle.
  return {
    scale,
    xPercent: -scale * (cx - 0.5) * 100,
    yPercent: -scale * (cy - 0.5) * 100,
    transformOrigin: "50% 50%",
  };
}

function reset() {
  if (shot) shot.kill();
  shot = null;
  clearGlows();
  hero.classList.remove("taken");
  novac.hidden = true;
  gsap.set(novac, { clearProps: "all" });
  gsap.set(holder, {
    clearProps: "all",
    xPercent: 0, yPercent: 0, scale: 1, autoAlpha: 1,
    transformOrigin: "50% 50%",
  });
}

/* ----------------------------------------------------------------------
   Which door the frame is on. `null` means wide - the whole cabinet.

   The push used to fire at 1.50s on arrival, every time, unasked. Two things
   are wrong with that once there is more than one box:

     - it answers a question the receiver has not asked yet. Arriving already
       pushed into door 04 hides the other seventeen doors and, worse, hides
       the fact that door 07 is also theirs. The wide shot is the one that
       says "here is the cabinet, and these two are yours";
     - it cannot be undone. An animation that plays itself is a film. The
       receiver is standing in front of the real cabinet and wants to look at
       one door, then the other, then back out.

   So it is a control now: tap a chip, or tap the door itself. Tap the same
   one again and the frame goes wide. Same timeline machinery, driven by a
   finger instead of a clock.
   ---------------------------------------------------------------------- */
/* Two variables, because they are two different questions.
   ------------------------------------------------------------------------
   `selected` is which box the PANEL and the BUTTON are about. In any state
   where you have a parcel here it is never null - the button has to name a
   door before it can be pressed, and a panel with nothing in it is the
   disappearing-picker fault this redesign exists to remove.

   `framed` is where the CAMERA is, and null means the wide shot. They used to
   be one variable, which is why going wide had to blank the caption: the
   screen forgot which door it was about the moment it stopped looking at it. */
let selected = null;
let framed = null;

/** The heading carries the constants. Per-door facts live in the panel. */
function heading() {
  if (mode === "full") {
    title.textContent = "Cabinet full";
    sub.textContent = "Nothing waiting, and no free box";
    return;
  }
  if (mode === "empty") {
    title.textContent = `${FREE.length} boxes free`;
    sub.textContent = "Nothing waiting for you";
    return;
  }
  // Words, not digits, for a small count in a sentence — the numerals on this
  // screen all mean "door", and "2 boxes" reads for a beat as one.
  const WORD = ["no", "one", "two", "three", "four", "five", "six"];
  const w = WORD[mine.length] || mine.length;
  title.textContent = mine.length > 1
    ? `${w[0].toUpperCase()}${w.slice(1)} boxes are yours`
    : `One box is yours`;
  sub.textContent = `${FREE.length} of 20 doors free`;
}

const tag = (name, cls, text) => {
  const n = document.createElement(name);
  if (cls) n.className = cls;
  if (text) n.textContent = text;
  return n;
};

/** The same `.tk-sm` ticket Home's second parcel uses. One box, one shape. */
function doorTicket(p) {
  const t = tag("div", "ticket tk-sm card");
  const body = tag("span", "tsbody");
  body.append(tag("b", "", "Dropped"), tag("span", "", p.at));

  const meter = tag("span", "tmeter");
  const fill = tag("i", p.soon ? "soon" : "");
  // Set rather than animated: motion.js drains `[data-left]` once, on load,
  // and this ticket is rebuilt every time the box changes.
  fill.style.transform = `scaleX(${p.pct})`;
  fill.style.transformOrigin = "left center";
  meter.append(fill);

  t.append(
    tag("span", "tsnum mono", pad(p.n)),
    body,
    tag("em", "tpill" + (p.soon ? " soon" : ""), p.left),
    meter,
  );
  return t;
}

function doorSwitch() {
  const w = tag("div", "dswitch recess");
  w.setAttribute("role", "group");
  w.setAttribute("aria-label", "Which of your boxes");
  mine.forEach((p) => {
    const b = tag("button");
    b.setAttribute("aria-pressed", String(selected === p.n));
    b.append(tag("b", "mono", pad(p.n)), tag("i", "dot" + (p.soon ? " soon" : "")));
    b.addEventListener("click", () => chooseDoor(p.n));
    w.append(b);
  });
  return w;
}

const ICON_FREE = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12.5l5 5L20 7"/></svg>';
const ICON_NONE = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><path d="M5.6 5.6l12.8 12.8"/></svg>';

function statusCard() {
  const free = mode === "empty";
  const c = tag("div", "cstatus card " + (free ? "free" : "none"));
  const icon = tag("span", "cicon");
  icon.innerHTML = free ? ICON_FREE : ICON_NONE;
  const text = tag("span");
  text.append(
    tag("b", "", free ? "Room for a drop" : "All 20 boxes in use"),
    tag("span", "", free
      ? "Give a courier your number and one is yours"
      : "A courier cannot leave one here"),
  );
  c.append(icon, text);
  return c;
}

function renderInfo() {
  cabinfo.textContent = "";
  if (mode === "empty" || mode === "full") {
    cabinfo.append(statusCard());
    return;
  }
  // The switch is built only when there is something to switch between.
  if (mine.length > 1) cabinfo.append(doorSwitch());
  cabinfo.append(doorTicket(mine.find((m) => m.n === selected) || mine[0]));
}

/** What the bottom of the screen offers, which is not the same in every state. */
function renderAction() {
  if (mode === "empty") {
    // Nothing of yours to open, and the cabinet already says it has room.
    // A disabled button here would be a control that exists to say no.
    action.hidden = true;
    return;
  }
  action.hidden = false;
  if (mode === "full") {
    go.disabled = false;
    go.classList.add("alt");
    gotext.textContent = "Show the Library cabinet";
    quiet.style.display = "none";
    return;
  }
  go.disabled = false;
  go.classList.remove("alt");
  gotext.textContent = `Scan to open ${pad(selected)}`;
  quiet.style.display = "";
}

/** Move the camera. Nothing else - see the note on `selected` above. */
function frameTo(n) {
  if (shot) shot.kill();
  const to = n === null
    ? { scale: 1, xPercent: 0, yPercent: 0, transformOrigin: "50% 50%" }
    : zoomFor(n);
  shot = gsap.to(holder, { ...to, duration: 0.72, ease: TRAVEL });
  if (slow) shot.timeScale(0.25);
}

/**
 * Pick a box: from the switch, or by tapping the door itself.
 *
 * Picking the one you are already looking at goes wide again - the receiver is
 * standing in front of the real cabinet and wants to look at one door, then
 * the other, then back out. The panel stays on that box either way, because
 * the button still has to name what it will open.
 */
function chooseDoor(n) {
  selected = n;
  framed = framed === n ? null : n;
  renderInfo();
  renderAction();
  frameTo(framed);
}

/** A tap on a lit door. */
function toggleDoor(n) {
  chooseDoor(n);
}

/**
 * Arrival, as one timeline - and it stops at the wide shot.
 *
 * This was four `setTimeout`s driving CSS transitions, and it worked until
 * anything needed to interrupt it. The bench has a slow-motion control and a
 * replay button, and switching mode mid-shot used to leave orphaned timers
 * that fired into the next take. A timeline has one playhead: `.kill()` is a
 * guarantee - which is exactly what a tap on a chip now needs.
 *
 *   0.00  the cabinet arrives from 94% and settles
 *   0.45  every door that is yours lights, one after another
 *   —     and it waits
 */
function play() {
  reset();
  framed = null;
  // `mine` first: the heading counts it, and reading it before the mode has
  // set it left "Two boxes are yours" over a one-parcel screen.
  mine = mode === "one" ? ALL_MINE.slice(0, 1) : ALL_MINE;
  heading();

  if (mode === "empty" || mode === "full") {
    selected = null;
    renderInfo();
    renderAction();

    if (mode === "full") {
      // Full is stated, not implied. See `.novac` in screens.css: an unlit
      // cabinet is also what loading looks like, and what a failed request
      // looks like, so absence cannot be the message.
      hero.classList.add("taken");
      novac.hidden = false;
      shot = gsap.from(novac, {
        autoAlpha: 0, scale: 0.9, duration: 0.4, ease: ARRIVE, delay: 0.28,
        transformOrigin: "center center",
      });
      if (slow) shot.timeScale(0.25);
      return;
    }

    // No parcel, no journey. The free doors are counted, not travelled to -
    // so they arrive together and the screen stays still. A push-in here
    // would be motion with nothing to say.
    FREE.forEach(markFree);
    shot = gsap.from([...faces.children, ...glows.children], {
      autoAlpha: 0, scale: 0.86, duration: 0.5,
      transformOrigin: "center center",
      stagger: { each: 0.045, from: "start" },
    });
    if (slow) shot.timeScale(0.25);
    return;
  }

  // The most urgent box is the one the button is about until you say otherwise.
  selected = mine[0].n;
  renderInfo();
  renderAction();

  shot = gsap.timeline();
  shot.from(holder, { scale: 0.94, autoAlpha: 0, duration: 0.43, ease: ARRIVE }, 0);
  shot.call(() => {
    clearGlows();
    mine.forEach((p) => lightDoor(p.n, cssVar("--door")));
    // The light comes UP rather than switching on. A door that snaps to amber
    // reads as a state that was always true; one that rises reads as a thing
    // that just happened to you, which is what a waiting parcel is. With two
    // doors they come up in order of urgency, which is the order to read them.
    gsap.from([...faces.children, ...glows.children], {
      autoAlpha: 0, duration: 0.34, ease: ARRIVE, stagger: 0.09,
    });
  }, null, 0.45);

  /* One box: push straight in. More than one: stay wide.
     --------------------------------------------------------------------
     The auto push-in was removed because with two parcels it answered a
     question nobody had asked and hid the fact that a second door was also
     yours. With ONE box there is no second door to hide and no choice to
     make - the wide shot asks the receiver to tap a door to find out what
     they already know. So the count decides it, which is the honest rule:
     the frame moves on its own only when there is exactly one place it
     could go. Tapping the door still pulls back out. */
  if (mine.length === 1) {
    const n = mine[0].n;
    shot.call(() => { framed = n; }, null, 1.02);
    shot.to(holder, { ...zoomFor(n), duration: 0.72, ease: TRAVEL }, 1.02);
  }

  if (slow) shot.timeScale(0.25);
}

const btnParcel = document.getElementById("btn-parcel");
const btnOne = document.getElementById("btn-one");
const btnEmpty = document.getElementById("btn-empty");
const btnFull = document.getElementById("btn-full");
const btnSlow = document.getElementById("btn-slow");

function setMode(next) {
  mode = next;
  btnParcel.setAttribute("aria-pressed", String(next === "parcel"));
  btnOne.setAttribute("aria-pressed", String(next === "one"));
  btnEmpty.setAttribute("aria-pressed", String(next === "empty"));
  btnFull.setAttribute("aria-pressed", String(next === "full"));
  play();
}

btnParcel.onclick = () => setMode("parcel");
btnOne.onclick = () => setMode("one");
btnEmpty.onclick = () => setMode("empty");
btnFull.onclick = () => setMode("full");
btnSlow.onclick = () => {
  slow = !slow;
  btnSlow.setAttribute("aria-pressed", String(slow));
  // No replay needed: the playhead is the same object either way.
  if (shot) shot.timeScale(slow ? 0.25 : 1);
};
document.getElementById("btn-replay").onclick = play;

play();

/* ======================================================================
   Theme.

   Dark mode is a setting **in the app**, so it changes the app. It used to
   change the browser page the phones stand on as well, which made a switch
   inside one screen look like the whole document had reloaded. The page
   tokens now follow the system and only the system - see screens.css.

   The wipe is Ryan's curtain component, at its own 550ms on
   cubic-bezier(0.76, 0, 0.24, 1), with two things changed:

     - it sits at z-index 0, BEHIND the screen's content, so nothing is ever
       covered. The ground wipes; the panels and the type stay in view and
       change underneath.
     - there is one per screen rather than one over the window.

   The amber screen is skipped. Its ground is `--door` in both schemes, so
   there is nothing there to wipe.
   ====================================================================== */

/* ======================================================================
   Every other picture of the cabinet is the same picture.

   The sign-in hero, the door crops on Home, and the free-boxes panel on the
   empty Home are all clones of the one <img> already in the page. One 1.5MB
   base64 for all of them: a second copy would have added the whole render
   again for a 62px thumbnail, and a hand-placed crop would drift out of
   alignment with the projection the moment the camera moved.
   ====================================================================== */
const CAB_SRC = document.getElementById("cabinet").getAttribute("src");

const cabClone = () => {
  const img = new Image();
  img.src = CAB_SRC;
  img.alt = "";
  return img;
};

document.querySelectorAll(".authhero").forEach((h) => h.append(cabClone()));


const toggles = [...document.querySelectorAll("[data-theme-toggle]")];

function currentTheme() {
  const stamped = document.documentElement.getAttribute("data-theme");
  return stamped === "dark" ? "dark" : "light";
}

function paintToggles(theme) {
  const dark = (theme || currentTheme()) === "dark";
  toggles.forEach((t) => t.setAttribute("aria-pressed", String(dark)));
}

// One curtain per screen, first child so it lands under everything else.
const curtains = [...document.querySelectorAll(".screen:not(.amber)")].map((s) => {
  const c = document.createElement("i");
  c.className = "curtain";
  c.setAttribute("aria-hidden", "true");
  s.prepend(c);
  return c;
});

// The component ships `duration: 550` as a prop and runs two passes with it,
// which is 1100ms. This runs one pass, at 480.
const CURTAIN_MS = 480;
const WAVE_MS = 300;         // how long the wave takes to cross a screen
let theming = 0;
let swapping = false;

/**
 * Give every block a delay from how far it sits from the edge the wave starts
 * at: the ceiling going dark, the floor going light.
 *
 * Measured per change rather than once, because a screen's layout moves - the
 * cabinet tab zooms, lists scroll - and a delay computed against a stale
 * position puts a block in the wrong place in the wave.
 */
/* An ARRAY, joined - not a string built with `+` down seven lines.
   ------------------------------------------------------------------------
   It was the string, and it broke the theme toggle outright. One `+` went
   missing between the last two fragments, so JavaScript read it as an
   assignment followed by a stray expression statement: perfectly legal, and
   `BLOCKS` silently ended on a trailing comma. `querySelectorAll` then threw
   on every theme change, `stageWave` died before `setTheme` could swap the
   attribute, and the toggle did nothing at all.

   `node --check` cannot catch that - there is nothing syntactically wrong
   with it - so the defence is to make the mistake unwriteable. A list has no
   glue to forget. */
const BLOCKS = [
  ".card", ".nav", ".appbar", ".authhero", ".authform", ".rank", ".divider",
  ".ways", ".legal", ".skip", ".action", ".heading", ".slabel", ".stack > *",
  ".grow > *", ".bignum", ".otp", ".display", ".authlede",
  // Home's own blocks. Left out, the whole relaid-out screen sits at
  // `--d: 0` and turns in one piece while everything around it waves -
  // which reads as the wave skipping Home.
  //
  // The ticket is staged as ONE block, not per child: `--d` is a custom
  // property, so it inherits, and everything inside a ticket turns on the
  // ticket's own delay. Staging its children as well would spread one card
  // across a third of the wave and read as the card coming apart.
  ".sheet > *", ".ticket", ".whereto", ".dswitch", ".cstatus", ".ledger > *",
].join(", ");

function stageWave(dir) {
  document.querySelectorAll(".screen").forEach((screen) => {
    const box = screen.getBoundingClientRect();
    if (!box.height) return;
    screen.querySelectorAll(BLOCKS).forEach((el) => {
      const r = el.getBoundingClientRect();
      // 0 for the block the wave reaches first, 1 for the last one it reaches.
      const far = dir === "down"
        ? (r.top - box.top) / box.height        // going dark: ceiling first
        : (box.bottom - r.bottom) / box.height; // going light: floor first
      const d = Math.round(Math.min(1, Math.max(0, far)) * WAVE_MS);
      el.style.setProperty("--d", `${d}ms`);
    });
  });
}

/**
 * What `--ground` is under each theme. Read once, at boot.
 *
 * This used to be measured on demand, by flipping `data-theme` to the target,
 * reading it, and flipping back. That is what stopped the transition running:
 * the flip happened in the same frame as the real change, so by the time the
 * attribute was set for real the style engine had already resolved the target
 * value in that frame and the tokens had nothing to move from. Every token
 * snapped, and only the curtain animated - which is exactly the "everything
 * freezes, then jumps" that the sequencing fix was supposed to cure.
 *
 * Measured here instead, before `.theming` exists and before anything can be
 * transitioning, and never touched again.
 */
const GROUND = (() => {
  const root = document.documentElement;
  const had = root.getAttribute("data-theme");
  const out = {};
  for (const theme of ["dark", "light"]) {
    root.setAttribute("data-theme", theme);
    out[theme] = getComputedStyle(root).getPropertyValue("--ground").trim();
  }
  if (had === null) root.removeAttribute("data-theme");
  else root.setAttribute("data-theme", had);
  return out;
})();

function setTheme(next) {
  if (swapping) return;
  swapping = true;
  const root = document.documentElement;

  // The switch throws NOW.
  //
  // It used to be painted when the theme landed, 550ms after the tap. That
  // was measured: the knob sat still through ms=485 and only began to travel
  // at ms=590. A switch that does not move under the finger reads as a dead
  // control, and half a second is long enough to press it twice.
  //
  // The knob is the acknowledgement; the wave is the consequence.
  paintToggles(next);

  /* Which way the change travels.
     --------------------------------------------------------------------
     Night falls from the top; day comes up from the floor. That is not a
     decorative choice - it is the only pair of directions that matches
     what the two words already mean, and a wipe that runs the same way
     both times reads as one mechanism playing twice rather than as two
     opposite events. */
  const dir = next === "dark" ? "down" : "up";

  // The wave is decoration; the theme change is the function. Staging must
  // never be able to stop the attribute swap - which is exactly what it did
  // when BLOCKS was malformed, and the failure looked like a dead toggle
  // rather than like a broken selector.
  try { stageWave(dir); } catch (e) { console.error("stageWave:", e); }

  // The curtain is filled with the colour the ground is LEAVING, and held
  // over it at full height. Everything that follows happens underneath it.
  //
  // It shrinks AWAY from the edge the new theme arrives at, so the origin is
  // the far edge: origin `bottom` and the curtain's top edge travels down,
  // uncovering the dark from the ceiling.
  curtains.forEach((c) => {
    c.style.background = GROUND[currentTheme()];
    c.style.transformOrigin = dir === "down" ? "bottom" : "top";
    c.classList.remove("wipe");
    c.classList.add("hold");
  });

  // Block transitions on, then swap. A forced read between the two, or they
  // land in one style batch, the transition is not yet in effect when the
  // values move, and every block snaps.
  //
  // The read has to be of a token. `void root.offsetWidth` flushes layout,
  // and a custom property does not affect layout, so it is not a reliable
  // way to make the engine resolve one. Asking for `--ground` by name is.
  root.classList.add("theming");
  getComputedStyle(root).getPropertyValue("--ground");
  root.setAttribute("data-theme", next);

  // Next frame the curtain shrinks, and its leading edge sweeps across the
  // screen uncovering the new ground. The blocks are already turning in that
  // same order, so the ground and the things standing on it change together
  // rather than one after the other.
  requestAnimationFrame(() => {
    curtains.forEach((c) => {
      c.classList.remove("hold");
      c.classList.add("wipe");
    });
  });

  clearTimeout(theming);
  theming = setTimeout(() => {
    root.classList.remove("theming");
    swapping = false;
  }, CURTAIN_MS + 80);
}

toggles.forEach((t) => {
  t.addEventListener("click", () => {
    setTheme(currentTheme() === "dark" ? "light" : "dark");
  });
});

/* The app opens light, and then belongs to the toggle.
   ------------------------------------------------------------------------
   It used to open in whatever scheme the machine was set to, which made the
   first thing anyone saw depend on a setting nobody in the room had chosen.
   Light is the daylight case - a receiver reads this screen outdoors, at a
   cabinet, in Binh Duong sun - so light is the one to be judged on and the
   one to open in. The system's scheme is not consulted at all, and the media
   query is gone with it: an app that follows the OS mid-session moves under
   the reader's hands for a reason that has nothing to do with the app.

   Stamped rather than left unset, because the light tokens are reached
   through [data-theme="light"] only. */
document.documentElement.setAttribute("data-theme", "light");
paintToggles();
