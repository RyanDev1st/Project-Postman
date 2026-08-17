/* The shipper's journey: a phone number, a name to confirm, a box that opens.
 *
 * P3-01 to P3-03 and P3-06. The metal half - P3-04 and P3-05 - is the same
 * code seen from here: it asks the server for a box and the server tells the
 * cabinet to open one. What this file cannot prove is that a door moved.
 *
 * ## It decides nothing
 *
 * It does not pick the box, does not check the number, and does not know
 * whose parcel anything is. Endpoint 10 answers who, endpoint 11 answers
 * which box. Rule 2 in architecture.md, and the reason there is exactly one
 * place the rules live.
 *
 * ## It is a public terminal
 *
 * Every sentence here is readable by whoever is standing behind the shipper.
 * The name comes back masked from the server and is never unmasked here; the
 * typed number is wiped the moment the step is left.
 */
(function () {
  "use strict";

  var net = window.CabinetNet;
  var root = document.getElementById("drop");
  if (!root || !net) return;

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

  /* Wipe on the way out, not on the way in. A number left in a variable is a
   * number the next screen can leak; clearing it when the next step starts
   * would leave it sitting there for as long as the shipper is reading. */
  function forget() {
    typed = "";
    receiverRef = "";
    document.getElementById("typed").textContent = "+84";
  }

  function trouble(say) {
    document.getElementById("trouble-say").textContent = say;
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
    document.getElementById("typed").textContent = "+84 " + out;

    var key = document.getElementById("find");
    key.textContent = "Find";
    key.disabled = typed.length !== DIGITS;
  }

  function pad() {
    var host = document.getElementById("pad");
    var keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "back", "0", "find"];

    keys.forEach(function (key) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "key";

      if (key === "back") {
        b.textContent = "⌫";
        b.setAttribute("aria-label", "Delete the last digit");
        b.addEventListener("click", function () {
          typed = typed.slice(0, -1);
          draw();
        });
      } else if (key === "find") {
        b.textContent = "Find";
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

  /* --- the two calls --------------------------------------------------- */

  function find() {
    var number = "+84" + typed;

    /* The key says what it is doing and stops taking taps. A shipper holding
     * a parcel gets no other signal that the cabinet heard them, and a
     * second tap here is a second request. */
    var key = document.getElementById("find");
    key.disabled = true;
    key.textContent = "Finding…";

    net
      .call("/cabinet/receiver?phone_number=" + encodeURIComponent(number))
      .then(function (answer) {
        if (answer.state === "ok") {
          receiverRef = answer.value.ref;
          document.getElementById("who").textContent = answer.value.masked_name;
          show("confirm");
          return;
        }
        if (answer.state === "refused") {
          /* Named, because "something went wrong" sends a shipper away with a
           * parcel and no idea what to do. Both of these have an action. */
          trouble(
            answer.code === "PHONE_NOT_REGISTERED"
              ? "That number has no account yet. Ask them to open the app first."
              : "That number was refused. Check it and try again."
          );
          return;
        }
        trouble("The server could not be reached. Try again in a moment.");
      });
  }

  /* The one call in this file that moves metal.
   *
   * It fires from this tap and from nowhere else - never a retry, never a
   * timer. The button is disabled first, so a shipper leaning on a
   * touchscreen sends one drop and not four. */
  function open() {
    var yes = document.getElementById("yes");
    yes.disabled = true;

    net
      .call("/cabinet/drop", {
        method: "POST",
        body: { receiver_ref: receiverRef, size: "medium" },
      })
      .then(function (answer) {
        yes.disabled = false;

        if (answer.state === "ok") {
          document.getElementById("boxno").textContent = answer.value.box_number;
          forget();
          show("open");
          return;
        }
        if (answer.state === "refused") {
          trouble(
            answer.code === "NO_FREE_BOX"
              ? "Every box here is full. Try another cabinet."
              : "The cabinet refused that. Start again."
          );
          return;
        }
        /* Unclear is not failure and must never be drawn as either. A drop
         * that timed out may have opened a door. Telling the shipper it
         * failed would send them away from a box with their parcel in it. */
        trouble("Not sure whether a box opened. Look at the cabinet before trying again.");
      });
  }

  /* --- wiring ---------------------------------------------------------- */

  pad();
  draw();

  document.getElementById("start").addEventListener("click", function () {
    if (!net.ready()) {
      trouble("This cabinet has no key yet. It cannot take a parcel.");
      return;
    }
    forget();
    draw();
    show("number");
  });

  document.getElementById("no").addEventListener("click", function () {
    forget();
    draw();
    show("number");
  });

  document.getElementById("yes").addEventListener("click", open);

  /* Endpoint 12. Today a person taps it, because there is no sensor
   * (ADR 0006). When the item sensors in ADR 0021 exist, the cabinet sees
   * the box fill and this button stops being the evidence. */
  document.getElementById("shut").addEventListener("click", function () {
    var box = document.getElementById("boxno").textContent;
    net.call("/cabinet/door-closed", {
      method: "POST",
      body: { box_number: box, purpose: "drop" },
    });
    show("idle");
  });

  document.getElementById("again").addEventListener("click", function () {
    forget();
    draw();
    show("idle");
  });
})();
