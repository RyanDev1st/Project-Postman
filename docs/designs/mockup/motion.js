/* ======================================================================
   Motion.

   ## The philosophy, and it is short

   A receiver opens this app about twice per parcel, for under twenty
   seconds, one-handed, often walking, sometimes in a corridor in the rain.
   That is the whole brief for motion, and three rules fall out of it.

   **1. Motion has to answer a question faster than stillness would.**
   Not decorate the answer - BE the answer. The cabinet sequence exists
   because "which of those metal doors is mine" is genuinely hard to say in
   words and trivial to say by lighting one and pushing into it. The expiry
   bars on Home drain rather than appear, because the thing the receiver
   needs is not the number of hours but the sense that one parcel is nearly
   out of time and the other is not. Anything that cannot pass this test is
   deleted rather than shortened.

   **2. One authored moment per screen. Everything else is feedback.**
   Feedback confirms a tap and gets out of the way - under 200ms, no
   overlap, no sequence. The authored moment is allowed to take a second and
   a half, and there is exactly one: the push into your door. A screen with
   two things competing to be looked at has no authored moment, it has two
   distractions.

   **3. Nothing bounces.**
   `back` and `elastic` are banned here, and not on taste grounds. This
   interface opens a locker containing someone's property, and an overshoot
   reads as *approximate*. Metal that springs is metal that is not latched.
   The one exception is the theme toggle, which is a supplied component and
   carries the component's own overshoot - a switch is a switch, and a
   switch is allowed to feel sprung.

   The ease is `power3.out` throughout, with `power2.inOut` for anything
   that travels a long way. Both are already in DESIGN.md as the loader
   film's curves, so the app and the film move the same way.

   ## Why GSAP and not CSS

   The cabinet sequence was four `setTimeout`s driving CSS transitions.
   That works right up to the moment anything has to be interrupted,
   reversed, slowed for inspection, or kept in step - and all four were
   needed. A timeline is one object with a playhead: `.timeScale(0.25)` is
   the slow-motion control on the bench, `.kill()` on a tab switch is a
   guarantee rather than four `clearTimeout`s and a hope.

   It also removes a whole class of bug this build has already paid for
   twice: a transition that silently does not run because the "before"
   style was never established in its own frame. GSAP records the start
   value itself.
   ====================================================================== */

const M = {
  /** The authored moment. Long enough to read, short enough to stand through. */
  hero: 1.5,
  /** Feedback. Confirms and leaves. */
  tap: 0.18,
  /** An entrance. */
  arrive: 0.62,
  /** How far a card travels in. Small - this is a settle, not a fly-in. */
  rise: 16,
};

gsap.defaults({ duration: M.arrive, ease: "power3.out" });

/* Respect the system setting, and mean it.

   `gsap.matchMedia()` reverts everything created inside a handler when its
   query stops matching, so the reduced-motion branch is not a flag checked
   in twenty places - it is a different set of animations entirely, and the
   others are torn down. */
const mm = gsap.matchMedia();
const REDUCED = "(prefers-reduced-motion: reduce)";
const FULL = "(prefers-reduced-motion: no-preference)";

/**
 * Home arrives as a wave of one.
 *
 * Every card carries the same 16px rise, so what distinguishes them is
 * ORDER, not treatment - which is the point. The reading order of Home is
 * the order of urgency: the parcel closest to expiry is the first thing that
 * moves, and the collected list, which nobody came here to read, is last.
 *
 * `from` rather than `fromTo`: the layout is already correct in the DOM, and
 * GSAP reads the resting state as the destination. Nothing has to be
 * duplicated in CSS, so nothing can drift out of step with it.
 */
function enterHome(screen) {
  const bits = screen.querySelectorAll("[data-enter]");
  if (!bits.length) return;
  return gsap.from(bits, {
    y: M.rise,
    autoAlpha: 0,
    duration: M.arrive,
    stagger: { each: 0.055, from: "start" },
    clearProps: "transform,opacity,visibility",
  });
}

/**
 * The expiry bars drain to where they actually are.
 *
 * They start full and run down to the parcel's real remaining fraction. A bar
 * that simply appeared at 12% would be a number in another costume; a bar
 * that *falls* to 12% while its neighbour barely moves says which parcel is
 * about to expire before the label has been read. That is rule 1.
 */
function drainBars(screen) {
  const bars = screen.querySelectorAll("[data-left]");
  if (!bars.length) return;
  return gsap.fromTo(bars,
    { scaleX: 1 },
    {
      scaleX: (i, el) => parseFloat(el.dataset.left),
      duration: 1.1,
      ease: "power2.inOut",
      stagger: 0.08,
      transformOrigin: "left center",
    });
}

/** Everything on Home, in the order Home should be read. */
function playHome(screen) {
  const tl = gsap.timeline();
  const enter = enterHome(screen);
  if (enter) tl.add(enter, 0);
  const drain = drainBars(screen);
  if (drain) tl.add(drain, 0.28);
  return tl;
}

mm.add({ full: FULL, reduced: REDUCED }, (ctx) => {
  const { full } = ctx.conditions;
  const homes = [...document.querySelectorAll(".screen")]
    .filter((s) => s.querySelector("[data-enter]"));

  if (!full) {
    // Reduced motion is not "the same thing, faster". Nothing travels and
    // nothing fades; the bars are simply drawn at their real value, which is
    // the information the motion existed to carry.
    homes.forEach((s) => {
      gsap.set(s.querySelectorAll("[data-enter]"), { clearProps: "all" });
      // The empty Home has no bars, and GSAP warns on an empty target list.
      const bars = s.querySelectorAll("[data-left]");
      if (bars.length) {
        gsap.set(bars, {
          scaleX: (i, el) => parseFloat(el.dataset.left),
          transformOrigin: "left center",
        });
      }
    });
    return;
  }

  homes.forEach(playHome);
});

// screens.js drives the cabinet sequence and needs these.
window.__motion = { M, mm, REDUCED, FULL, playHome };
