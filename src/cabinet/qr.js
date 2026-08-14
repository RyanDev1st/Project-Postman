/* P5-01 — the QR session code on the cabinet screen.
 *
 * The receiver's phone scans THIS screen. The phone never shows a code for
 * the cabinet to read - see ADR 0003 and architecture.md section 6.
 *
 * WHAT THE QR HOLDS, AND WHY IT IS NOT A KEY
 *
 * Two facts: which cabinet, and at what moment. Nothing else. P0-07 settled
 * this on 2026-08-05 and the wording there is exact - "the code itself only
 * says which cabinet and when". Identity comes from the token in the app, so
 * a photograph of this screen opens nothing: whoever holds the photo still
 * has to be signed in as the person the parcel belongs to.
 *
 * That is the whole reason the timings below are safe to be generous. A
 * typed code is a secret and must be short-lived; this is a location and a
 * clock reading, and the only thing the clock stops is somebody scanning a
 * screenshot from last Tuesday.
 *
 * THE PAYLOAD
 *
 *     VGU1|<cabinet-id>|<unix-seconds>
 *
 * `VGU1` is a format marker, so the app can refuse anything that is not ours
 * and so the format can change later without a scanner that guesses. Pipes
 * rather than JSON because every byte is a module on the screen, and a
 * smaller code scans from further away and in worse light - which is exactly
 * what P5-02 is checked against.
 *
 * NO NETWORK HERE. net.js is the only file that may reach out, and
 * scripts/checknet.py fails the build if that stops being true. This file
 * needs nothing from a server: it already knows which cabinet it is and what
 * time it is, and those are the only two facts in the code.
 */
(function () {
  "use strict";

  var cfg = window.CABINET_CONFIG || {};
  var build = window.CABINET_BUILD || {};

  /* Both numbers are guesses from P0-07 and both live in
   * `config/settings.json` as the source of truth. They are repeated here as
   * defaults so a cabinet with no config still draws something honest, and
   * they are overridable per device so a cabinet in a dark corridor can be
   * changed without a release. */
  var SESSION_SECONDS = cfg.QR_SESSION_SECONDS || 60;
  var REFRESH_SECONDS = cfg.QR_REFRESH_SECONDS || 30;

  var FORMAT = "VGU1";

  var canvas = document.getElementById("qr");
  var note = document.getElementById("qr-note");
  var count = document.getElementById("qr-count");
  if (!canvas) return;

  /* Which cabinet this is. Placed on the device at setup, never built in.
   *
   * With no id there is nothing true to encode. A screen that invents one
   * would send a phone to a cabinet that does not exist, so it draws no code
   * and says why - the same rule the footer already follows. */
  var cabinetId = build.CABINET_ID || cfg.CABINET_ID || null;

  var expiresAt = 0;

  function payloadFor(seconds) {
    return FORMAT + "|" + cabinetId + "|" + seconds;
  }

  /* Say why there is no code, instead of showing one. The countdown goes
   * with it: a clock ticking under a blank square would read as "nearly
   * ready", and it is not going to be ready. */
  function say(reason) {
    note.textContent = reason;
    count.hidden = true;
    canvas.hidden = true;
  }

  function draw() {
    var seconds = Math.floor(Date.now() / 1000);
    expiresAt = seconds + SESSION_SECONDS;

    /* Error correction M, not H. H spends a quarter of the code on recovery
     * and makes the modules smaller for the same panel; this code is on a
     * clean backlit screen a arm's length away, not printed on a box that
     * gets scuffed. M keeps the modules large, and large is what reads in a
     * dark corridor. */
    window.QRCode.toCanvas(
      canvas,
      payloadFor(seconds),
      { errorCorrectionLevel: "M", margin: 2, scale: 8, color: { dark: "#0f1115", light: "#ffffff" } },
      function (err) {
        if (err) {
          /* Never a blank white square. A blank square is indistinguishable
           * from a code that will not scan, and a person would stand there
           * trying. */
          say("Cannot draw the code");
        }
      }
    );
  }

  function tick() {
    if (!cabinetId) return;
    var left = expiresAt - Math.floor(Date.now() / 1000);
    if (left < 0) left = 0;
    count.textContent = left + "s";
  }

  if (!cabinetId) {
    /* Honest about being unset, exactly as the footer is. */
    say("This cabinet has no id yet");
    return;
  }

  draw();
  tick();
  setInterval(draw, REFRESH_SECONDS * 1000);
  setInterval(tick, 1000);

  /* Coming back from a blanked or locked screen, the code on it is stale by
   * however long the screen was off. Redraw on wake rather than waiting out
   * the rest of the interval, so nobody ever scans a code that expired while
   * the screen was dark. */
  document.addEventListener("visibilitychange", function () {
    if (!document.hidden) draw();
  });
})();
