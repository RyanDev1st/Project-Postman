/* P5-01 — the QR session code on the cabinet screen.
 *
 * The receiver's phone scans THIS screen. The phone never shows a code for
 * the cabinet to read - see ADR 0003 and architecture.md section 6.
 *
 * WHAT THE QR HOLDS, AND WHY IT IS NOT A KEY
 *
 * Which cabinet, at what moment, and a random field only the server can
 * check. Identity comes from the token in the app, so a photograph of this
 * screen opens nothing: whoever holds the photo still has to be signed in as
 * the person the parcel belongs to.
 *
 * THE PAYLOAD
 *
 *     VGU1|<cabinet-id>|<unix-seconds>|<random>
 *
 * **The server writes it, at endpoint 9.** This screen asks for one and draws
 * what it is given. It composed its own until 2026-08-15, and that could not
 * survive the server checking codes: a code the server never issued is a code
 * the server must refuse, so a screen that invents them sends every scanner
 * away. `Sessions.format` on the server is the one place the shape is decided;
 * the app's `SessionCode.kt` reads it; this file only carries it.
 *
 * Pipes rather than JSON because every byte is a module on the screen, and a
 * smaller code scans from further away and in worse light - which is exactly
 * what P5-02 is checked against.
 *
 * The request goes through net.js, like every other request this screen makes.
 * scripts/checknet.py fails the build if any file here opens its own.
 */
(function () {
  "use strict";

  var cfg = window.CABINET_CONFIG || {};
  var build = window.CABINET_BUILD || {};

  /* How often to ask for a fresh code. A guess from P0-07, living in
   * `config/settings.json`, repeated here as a default so a cabinet with no
   * config still behaves, and overridable per device so a screen in a dark
   * corridor can be changed without a release. */
  var REFRESH_SECONDS = cfg.QR_REFRESH_SECONDS || 30;

  var canvas = document.getElementById("qr");
  var note = document.getElementById("qr-note");
  var count = document.getElementById("qr-count");
  if (!canvas) return;

  /* Which cabinet this is. Placed on the device at setup, never built in. */
  var cabinetId = build.CABINET_ID || cfg.CABINET_ID || null;

  var expiresAt = 0;
  var asking = false;

  /* Say why there is no code, instead of showing one. The countdown goes with
   * it: a clock ticking under a blank square would read as "nearly ready",
   * and it is not going to be ready. */
  function say(reason) {
    note.textContent = reason;
    count.hidden = true;
    canvas.hidden = true;
    expiresAt = 0;
  }

  function show(payload, livesSeconds) {
    /* Error correction M, not H. H spends a quarter of the code on recovery
     * and makes the modules smaller for the same panel; this code is on a
     * clean backlit screen an arm's length away, not printed on a box that
     * gets scuffed. M keeps the modules large, and large is what reads in a
     * dark corridor. */
    window.QRCode.toCanvas(
      canvas,
      payload,
      { errorCorrectionLevel: "M", margin: 2, scale: 8, color: { dark: "#0f1115", light: "#ffffff" } },
      function (err) {
        if (err) {
          /* Never a blank white square. A blank square is indistinguishable
           * from a code that will not scan, and a person would stand there
           * trying. */
          say("Cannot draw the code");
          return;
        }
        note.textContent = "";
        canvas.hidden = false;
        count.hidden = false;
        expiresAt = Math.floor(Date.now() / 1000) + (livesSeconds || 60);
      }
    );
  }

  /* Ask the server for a code and draw it.
   *
   * Three answers, three behaviours, and the middle one is the reason this is
   * not a one-liner: an unreachable server must NOT leave the last code on
   * screen. That code will expire, and a student scanning it would be refused
   * by a server they cannot see, with nothing on the screen to explain why. */
  function refresh() {
    if (asking) return;
    asking = true;

    window.CabinetNet.call("/cabinet/session").then(function (answer) {
      asking = false;

      if (answer.state === "ok" && answer.value && answer.value.session_code) {
        show(answer.value.session_code, answer.value.lives_seconds);
        return;
      }
      if (answer.state === "refused") {
        /* The one that matters here is CABINET_UNKNOWN: this screen's key is
         * not one the server issued. Naming it plainly saves somebody an
         * afternoon at a screen that looks broken. */
        say(
          answer.code === "CABINET_UNKNOWN"
            ? "This cabinet's key is not recognised"
            : "The server refused: " + answer.code
        );
        return;
      }
      say("Waiting for the server");
    });
  }

  function tick() {
    if (!expiresAt) return;
    var left = expiresAt - Math.floor(Date.now() / 1000);
    if (left < 0) left = 0;
    count.textContent = left + "s";

    /* Expired and no new one arrived - the server has been unreachable for a
     * whole cycle. Stop showing a code that cannot work. */
    if (left === 0) say("Waiting for the server");
  }

  if (!cabinetId) {
    say("This cabinet has no id yet");
    return;
  }
  if (!window.CabinetNet.ready()) {
    say("This cabinet has no key or no server address yet");
    return;
  }

  say("Asking the server for a code");
  refresh();
  setInterval(refresh, REFRESH_SECONDS * 1000);
  setInterval(tick, 1000);

  /* Coming back from a blanked or locked screen, the code on it is stale by
   * however long the screen was off. Ask again on wake rather than waiting
   * out the rest of the interval, so nobody ever scans a code that expired
   * while the screen was dark. */
  document.addEventListener("visibilitychange", function () {
    if (!document.hidden) refresh();
  });
})();
