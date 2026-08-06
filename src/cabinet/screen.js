/* Fill in the footer, and keep the size readout live.
 *
 * P1-03 only. No network call - that is P1-06.
 */
(function () {
  "use strict";

  var build = window.CABINET_BUILD || {};

  document.getElementById("version").textContent = build.VERSION || "unknown";

  /* An unset cabinet id shows as a dash, not as a made-up name. A screen
   * that invents an identity is worse than one that admits it has none. */
  document.getElementById("cabinet-id").textContent = build.CABINET_ID || "not set";

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
