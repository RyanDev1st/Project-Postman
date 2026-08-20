/* Fill in the footer, and keep the size readout live.
 *
 * P1-03 only. No network call - that is P1-06.
 */
(function () {
  "use strict";

  var build = window.CABINET_BUILD || {};
  var config = window.CABINET_CONFIG || {};

  document.getElementById("version").textContent = build.VERSION || "unknown";

  /* An unset cabinet id shows as a dash, not as a made-up name. A screen
   * that invents an identity is worse than one that admits it has none.
   *
   * **The config is asked first, because the config is the one that is real.**
   * `CABINET_BUILD.CABINET_ID` is null in the repo on purpose and is meant to
   * be placed on the device at setup; `CABINET_CONFIG.CABINET_ID` is what
   * net.js actually signs its calls as. Reading only the build meant the
   * footer said "not set" on a screen that was working perfectly well as
   * vgu-back-gate - so nobody standing at it could tell which cabinet it was.
   * Seen on Ryan's screenshot, 2026-08-17. */
  document.getElementById("cabinet-id").textContent =
    config.CABINET_ID || build.CABINET_ID || "not set";

  /* The live size readout. P1-03 has to be proved at two screen sizes, and
   * this is what makes that a thing you read off the screen instead of a
   * thing you claim. Resize the window and it follows. */
  var out = document.getElementById("size");

  function showSize() {
    out.textContent = window.innerWidth + " x " + window.innerHeight;
  }

  showSize();
  window.addEventListener("resize", showSize);
})();
