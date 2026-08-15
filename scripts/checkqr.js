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

/* The browser, only as far as qr.js reaches into it - plus CabinetNet, which
 * stands in for the server.
 *
 * `answer` is what net.js would have resolved to. It is passed in so the same
 * stub covers the three cases the screen must tell apart: a code, a refusal,
 * and a server that cannot be reached. */
function fakeDom(cabinetId, answer) {
  const els = {};
  for (const id of ["qr", "qr-note", "qr-count"]) {
    els[id] = { id, hidden: false, textContent: "", width: 264, height: 264 };
  }
  const drawn = [];
  const timers = [];
  const asked = [];
  const g = {
    CABINET_CONFIG: {
      CABINET_ID: cabinetId,
      QR_SESSION_SECONDS: 60,
      QR_REFRESH_SECONDS: 30,
    },
    CABINET_BUILD: { VERSION: "test", CABINET_ID: null },
    CabinetNet: {
      ready: () => Boolean(cabinetId),
      call(path) {
        asked.push(path);
        /* Resolved, never rejected - the same promise contract net.js keeps,
         * so a missing catch here would be a missing catch there. */
        return Promise.resolve(answer);
      },
    },
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
  return { g, els, drawn, timers, asked };
}

/* Run the shipped file against that stub.
 *
 * Async, because the screen now asks the server before it draws anything. The
 * await lets the promise the stub resolved actually settle before anything is
 * checked - without it every assertion would run against a screen that is
 * still waiting, and pass or fail for the wrong reason. */
async function run(cabinetId, answer) {
  const src = fs.readFileSync(path.join(CABINET, "qr.js"), "utf8");
  const { g, els, drawn, timers, asked } = fakeDom(cabinetId, answer);
  const fn = new Function(
    "window", "document", "setInterval", "Date",
    "var self = window; " + src
  );
  fn(g, g.document, g.setInterval, Date);
  await new Promise((r) => setImmediate(r));
  return { els, drawn, timers, asked };
}

/* A code shaped the way the server's Sessions.format writes them. */
function serverCode(cabinetId) {
  return ["VGU1", cabinetId, Math.floor(Date.now() / 1000), "cr7Qk2p0aXZlLXJhbmRvbS1ieXRlcw"].join("|");
}

/* --- a cabinet that knows which cabinet it is ------------------------- */

async function main() {

const issued = serverCode("vgu-test-01");
const set = await run("vgu-test-01", { state: "ok", value: { session_code: issued, lives_seconds: 60 } });

check(set.asked.includes("/cabinet/session"), `asked for ${JSON.stringify(set.asked)}, expected /cabinet/session`);
check(set.drawn.length === 1, `drew ${set.drawn.length} codes on load, expected 1`);

const payload = set.drawn.length ? set.drawn[0].payload : "";
console.log("payload: " + JSON.stringify(payload));

/* The screen draws what the server sent, byte for byte. Anything else - a
 * re-encoding, a trim, a "helpful" rebuild from the parts - is a code the
 * server will not recognise, because the server compares the whole string. */
check(payload === issued, "the screen did not draw exactly the code the server issued");

/* The code is a place, a moment and a random field, never a secret of the
 * cabinet's. If the cabinet KEY ever reached this payload it would be on a
 * public screen, photographable by anyone standing in the lobby. */
check(!/key|token|bearer/i.test(payload), "payload contains something that reads like a cabinet credential");

/* Error correction M, not H: M keeps the modules large, and large is what
 * reads in the dark corridor P5-02 is checked in. */
const opts = set.drawn.length ? set.drawn[0].opts : {};
check(opts.errorCorrectionLevel === "M", `error correction is ${opts.errorCorrectionLevel}, expected M`);
check(opts.margin >= 2, `quiet zone margin is ${opts.margin}, expected at least 2 modules`);

/* Two timers: ask again, and the countdown. Both from the settings, in ms. */
check(set.timers.includes(30 * 1000), `no 30s refresh timer, got ${JSON.stringify(set.timers)}`);
check(set.timers.includes(1000), `no 1s countdown timer, got ${JSON.stringify(set.timers)}`);

check(set.els["qr"].hidden === false, "the canvas is hidden on a cabinet that has a code");

/* --- a server that will not answer ------------------------------------ */

/* The one that matters. An unreachable server must not leave the last code on
 * screen: it will expire, and a student scanning it gets refused by a server
 * they cannot see, with nothing on the screen to explain why. */
const down = await run("vgu-test-01", { state: "unclear", why: "could not reach the server" });
check(down.drawn.length === 0, "drew a code while the server was unreachable - it cannot have come from the server");
check(down.els["qr"].hidden === true, "left a canvas up with no code behind it");
check(/server/i.test(down.els["qr-note"].textContent), `an unreachable server says ${JSON.stringify(down.els["qr-note"].textContent)}, which does not explain itself`);

/* --- a cabinet whose key the server does not know --------------------- */

const rejected = await run("vgu-test-01", { state: "refused", code: "CABINET_UNKNOWN" });
check(rejected.drawn.length === 0, "drew a code after the server refused this cabinet's key");
check(/key/i.test(rejected.els["qr-note"].textContent), `a refused key says ${JSON.stringify(rejected.els["qr-note"].textContent)}, which does not name the problem`);

/* --- a cabinet nobody has named yet ----------------------------------- */

const unset = await run(null, { state: "ok", value: { session_code: serverCode("x"), lives_seconds: 60 } });
check(unset.drawn.length === 0, "drew a code for a cabinet with no id - it would send a phone nowhere");
check(unset.asked.length === 0, "asked the server for a code before knowing which cabinet it is");
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
console.log("cabinet QR: it carries the server's code, and says so plainly when there is not one");

}

main();
