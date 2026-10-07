/* The shipper's journey: a phone number, a person to confirm, a box that opens.
 *
 * P3-01 to P3-03, P3-06 and P3-09 to P3-12.
 *
 * **It decides nothing.** It does not pick the box, does not check the
 * number, does not measure how close one number is to another. Endpoint 10
 * answers who, 28 answers the name, 11 answers which box. Rule 2 in
 * architecture.md, and the reason the rules live in exactly one place.
 *
 * The ladder it walks is `Ladder.kt` on the server; the four answers here:
 *
 *   exact / booking  -> "Is this the right person?"   a statement
 *   near             -> "Did you mean this person?"   a question
 *   refused          -> type the name off the label
 *   name refused     -> nobody. Call them, go to ABO
 *
 * **It is a public terminal.** Every sentence is readable by whoever stands
 * behind the shipper. The name comes back masked and is never unmasked here;
 * the typed number and name are wiped on the way out of a step.
 */
(function () {
  "use strict";

  var net = window.CabinetNet;
  var text = window.CabinetText;
  var root = document.getElementById("drop");
  if (!root || !net || !text) return;

  var steps = {};
  Array.prototype.forEach.call(root.querySelectorAll(".step"), function (el) {
    steps[el.getAttribute("data-step")] = el;
  });

  var typed = "";
  var receiverRef = "";

  function show(name) {
    Object.keys(steps).forEach(function (key) {
      steps[key].hidden = key !== name;
    });
  }

  function at(id) {
    return document.getElementById(id);
  }

  /* Wipe on the way out, not on the way in. A number left in a variable is a
   * number the next screen can leak; clearing it when the next step starts
   * would leave it sitting there for as long as the shipper is reading. */
  function forget() {
    typed = "";
    receiverRef = "";
    at("typed").textContent = "+84";
    at("name-in").value = "";
  }

  function trouble(key) {
    at("trouble-say").textContent = text.t(key);
    forget();
    show("trouble");
  }

  /* --- the number ------------------------------------------------------ */

  /* Nine digits after +84, which is every Vietnamese mobile. Typing stops
   * there rather than letting the shipper run on and be refused by the
   * server for a length the screen already knew was wrong. */
  var DIGITS = 9;

  function draw() {
    /* Grouped 3-3-3 as it is typed. Reading a nine-digit run back off a
     * screen to check it is exactly where a shipper makes a mistake. */
    var out = typed.replace(/(\d{3})(\d{0,3})(\d{0,3})/, function (all, a, b, c) {
      return [a, b, c].filter(Boolean).join(" ");
    });
    at("typed").textContent = "+84 " + out;

    var key = at("find");
    key.textContent = text.t("number.find");
    key.disabled = typed.length !== DIGITS;
  }

  function pad() {
    var host = at("pad");
    var keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "back", "0", "find"];

    keys.forEach(function (key) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "key";

      if (key === "back") {
        b.textContent = "⌫";
        b.setAttribute("data-t-label", "number.back");
        b.setAttribute("aria-label", text.t("number.back"));
        b.addEventListener("click", function () {
          typed = typed.slice(0, -1);
          draw();
        });
      } else if (key === "find") {
        b.textContent = text.t("number.find");
        b.id = "find";
        b.className = "key find";
        b.addEventListener("click", find);
      } else {
        b.textContent = key;
        b.addEventListener("click", function () {
          if (typed.length < DIGITS) {
            typed += key;
            draw();
          }
        });
      }
      host.appendChild(b);
    });
  }

  /* --- the ladder ------------------------------------------------------ */

  /* What the server said, drawn as the right question.
   *
   * `match` is the server's word for how sure it is, and it decides which
   * step is shown - never anything this file works out for itself. */
  function met(found) {
    receiverRef = found.ref;
    if (found.match === "near") {
      at("who-near").textContent = found.masked_name;
      show("near");
      return;
    }
    at("who").textContent = found.masked_name;
    show("confirm");
  }

  function find() {
    var number = "+84" + typed;

    /* The key says what it is doing and stops taking taps. A shipper holding
     * a parcel gets no other signal that the cabinet heard them, and a
     * second tap here is a second request. */
    var key = at("find");
    key.disabled = true;
    key.textContent = text.t("number.finding");

    net
      .call("/cabinet/receiver?phone_number=" + encodeURIComponent(number))
      .then(function (answer) {
        if (answer.state === "ok") {
          met(answer.value);
          return;
        }
        if (answer.state === "refused") {
          /* Rung D. `PHONE_NOT_REGISTERED` here does NOT mean give up: it
           * covers a number close to nobody and a number close to two
           * people, and both are answered the same way - ask for the name.
           * ABO comes later, from endpoint 28, and not from here. */
          if (answer.code === "PHONE_NOT_REGISTERED") {
            draw();
            at("name-in").value = "";
            show("name");
            return;
          }
          trouble("trouble.refused");
          return;
        }
        trouble("trouble.offline");
      });
  }

  /* Rung D, the second half. One guess, one answer. */
  function checkName() {
    var name = at("name-in").value.trim();
    if (!name) return;

    var key = at("name-go");
    key.disabled = true;
    key.textContent = text.t("name.checking");

    net
      .call("/cabinet/confirm-name", {
        method: "POST",
        body: { phone_number: "+84" + typed, name: name },
      })
      .then(function (answer) {
        key.disabled = false;
        key.textContent = text.t("name.check");

        if (answer.state === "ok") {
          met(answer.value);
          return;
        }
        /* Rung E, and the only sentence on this screen that sends somebody
         * away - so it says exactly where to go. */
        if (answer.state === "refused") {
          trouble("abo.say");
          return;
        }
        trouble("trouble.offline");
      });
  }

  /* --- the one call that moves metal ----------------------------------- */

  /* It fires from a tap and from nowhere else - never a retry, never a
   * timer. The button is disabled first, so a shipper leaning on a
   * touchscreen sends one drop and not four. */
  function open() {
    var yes = at("yes");
    var nearYes = at("near-yes");
    yes.disabled = true;
    nearYes.disabled = true;

    net
      .call("/cabinet/drop", {
        method: "POST",
        body: { receiver_ref: receiverRef, size: "medium", typed_number: "+84" + typed },
      })
      .then(function (answer) {
        yes.disabled = false;
        nearYes.disabled = false;

        if (answer.state === "ok") {
          at("boxno").textContent = answer.value.box_number;
          forget();
          show("open");
          return;
        }
        if (answer.state === "refused") {
          trouble(answer.code === "NO_FREE_BOX" ? "trouble.full" : "trouble.cabinet");
          return;
        }
        /* Unclear is not failure and must never be drawn as either. A drop
         * that timed out may have opened a door. Telling the shipper it
         * failed would send them away from a box with their parcel in it. */
        trouble("trouble.unclear");
      });
  }

  /* --- wiring ---------------------------------------------------------- */

  function backToNumber() {
    forget();
    draw();
    show("number");
  }

  pad();
  text.apply();
  draw();

  at("start").addEventListener("click", function () {
    if (!net.ready()) {
      trouble("trouble.noKey");
      return;
    }
    backToNumber();
  });

  at("no").addEventListener("click", backToNumber);
  at("near-no").addEventListener("click", backToNumber);
  at("name-back").addEventListener("click", backToNumber);

  at("yes").addEventListener("click", open);
  at("near-yes").addEventListener("click", open);
  at("name-go").addEventListener("click", checkName);
  at("name-in").addEventListener("keydown", function (e) {
    if (e.key === "Enter") checkName();
  });

  /* Endpoint 12. Today a person taps it, because there is no sensor
   * (ADR 0006). When the item sensors in ADR 0021 exist, the cabinet sees
   * the box fill and this button stops being the evidence. */
  at("shut").addEventListener("click", function () {
    var box = at("boxno").textContent;
    net.call("/cabinet/door-closed", {
      method: "POST",
      body: { box_number: box, purpose: "drop" },
    });
    show("idle");
  });

  at("again").addEventListener("click", function () {
    forget();
    draw();
    show("idle");
  });

  /* One tap changes the whole screen. The keypad's Find key is redrawn from
   * here because its label lives on a button this file made rather than in
   * the HTML, so `data-t` never sees it. */
  text.onChange(draw);
  at("lang").addEventListener("click", function () {
    text.use(text.other());
  });
})();
