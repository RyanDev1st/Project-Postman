/* Prove the cabinet screen draws a QR that scans, and that it says the right
 * thing.
 *
 *     node scripts/checkqr.js
 *
 * Exit code 0 means clean. Anything else means read the output.
 *
 * WHY THIS EXISTS
 *
 * The QR is the one thing on the cabinet screen a person cannot check by
 * looking at it. A wrong colour is obvious across the room; a code carrying
 * the wrong cabinet id, or a stale moment, or a payload the app will refuse,
 * looks exactly like a correct one. It is a picture that has to be read by a
 * machine, so a machine has to read it here.
 *
 * WHAT IT RUNS
 *
 * The real `src/cabinet/qr.js`, unmodified, in a stub DOM. Not a copy of its
 * logic - a copy would agree with itself while the shipped file was wrong.
 * The stub stands in for the browser only where the browser is the thing
 * being faked: the elements, and the canvas call. What the file computes is
 * its own.
 *
 * Decoding happens in scripts/checkqr.py, which reads the bitmap back with an
 * unrelated decoder. This half proves the payload; that half proves it can be
 * read at all.
 */
"use strict";

const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "..");
const CABINET = path.join(ROOT, "src", "cabinet");

const problems = [];
function check(ok, what) {
  if (!ok) problems.push("  " + what);
}

/* The browser, only as far as qr.js reaches into it. */
function fakeDom(cabinetId) {
  const els = {};
  for (const id of ["qr", "qr-note", "qr-count"]) {
    els[id] = { id, hidden: false, textContent: "", width: 264, height: 264 };
  }
  const drawn = [];
  const timers = [];
  const g = {
    CABINET_CONFIG: {
      CABINET_ID: cabinetId,
      QR_SESSION_SECONDS: 60,
      QR_REFRESH_SECONDS: 30,
    },
    CABINET_BUILD: { VERSION: "test", CABINET_ID: null },
    QRCode: {
      toCanvas(canvas, payload, opts, done) {
        drawn.push({ payload, opts, canvas });
        if (done) done(null);
      },
    },
    document: {
      getElementById: (id) => els[id] || null,
      addEventListener: () => {},
      hidden: false,
    },
    setInterval: (fn, ms) => {
      timers.push(ms);
      return timers.length;
    },
  };
  g.window = g;
  return { g, els, drawn, timers };
}

/* Run the shipped file against that stub. */
function run(cabinetId) {
  const src = fs.readFileSync(path.join(CABINET, "qr.js"), "utf8");
  const { g, els, drawn, timers } = fakeDom(cabinetId);
  const fn = new Function(
    "window", "document", "setInterval", "Date",
    "var self = window; " + src
  );
  fn(g, g.document, g.setInterval, Date);
  return { els, drawn, timers };
}

/* --- a cabinet that knows which cabinet it is ------------------------- */

const now = Math.floor(Date.now() / 1000);
const set = run("vgu-test-01");

check(set.drawn.length === 1, `drew ${set.drawn.length} codes on load, expected 1`);

const payload = set.drawn.length ? set.drawn[0].payload : "";
console.log("payload: " + JSON.stringify(payload));

const parts = payload.split("|");
check(parts.length === 3, `payload has ${parts.length} fields, expected 3`);
check(parts[0] === "VGU1", `format marker is ${JSON.stringify(parts[0])}, expected "VGU1"`);
check(parts[1] === "vgu-test-01", `cabinet id is ${JSON.stringify(parts[1])}, expected the configured one`);

const stamp = Number(parts[2]);
check(Number.isInteger(stamp), `moment ${JSON.stringify(parts[2])} is not a whole number of seconds`);
check(Math.abs(stamp - now) <= 2, `moment is ${stamp - now}s off the clock, expected now`);

/* The code is a location and a clock reading, never a secret. If a key ever
 * reaches this payload it is on a public screen, photographable by anyone
 * standing in the lobby. */
check(!/key|token|secret|bearer/i.test(payload), "payload contains something that reads like a secret");

/* Error correction M, not H: M keeps the modules large, and large is what
 * reads in the dark corridor P5-02 is checked in. */
const opts = set.drawn.length ? set.drawn[0].opts : {};
check(opts.errorCorrectionLevel === "M", `error correction is ${opts.errorCorrectionLevel}, expected M`);
check(opts.margin >= 2, `quiet zone margin is ${opts.margin}, expected at least 2 modules`);

/* Two timers: redraw, and the countdown. Both from the settings, in ms. */
check(set.timers.includes(30 * 1000), `no 30s redraw timer, got ${JSON.stringify(set.timers)}`);
check(set.timers.includes(1000), `no 1s countdown timer, got ${JSON.stringify(set.timers)}`);

check(set.els["qr"].hidden === false, "the canvas is hidden on a cabinet that has an id");

/* --- a cabinet nobody has named yet ----------------------------------- */

const unset = run(null);
check(unset.drawn.length === 0, "drew a code for a cabinet with no id - it would send a phone nowhere");
check(unset.els["qr"].hidden === true, "left a blank canvas up instead of hiding it");
check(/no id/i.test(unset.els["qr-note"].textContent), `unset cabinet says ${JSON.stringify(unset.els["qr-note"].textContent)}, which does not explain itself`);
check(unset.els["qr-count"].hidden === true, "left a countdown ticking under a code that is never coming");

/* --- verdict ---------------------------------------------------------- */

if (problems.length) {
  console.log("\nthe cabinet QR is wrong:");
  console.log(problems.join("\n"));
  console.log("\nnot clean");
  process.exit(1);
}
console.log("cabinet QR: payload, timings and the unset case all correct");
