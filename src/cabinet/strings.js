/* Every sentence on the cabinet screen, in both languages. Task P3-12.
 *
 * **Vietnamese is the default and English is the option**, not the other way
 * round. This screen stands in a lobby on a Vietnamese campus and is read by
 * a courier holding a parcel, usually in a hurry. Until 2026-09-03 every
 * sentence was hardcoded English behind `<html lang="en">`, which meant the
 * people it was built for read it second.
 *
 * ## How it works
 *
 * An element carrying `data-t="key"` has its text replaced by `t(key)`. The
 * HTML still holds the Vietnamese words, so the screen reads correctly with
 * this file missing or before it runs - a blank screen is worse than an
 * un-switched one.
 *
 * There is no build step here and there never will be (ADR 0009), so this is
 * a plain object rather than a `.properties` file and a loader.
 *
 * ## The rules
 *
 * - Every key exists in both languages. `check.py` for this is the test at
 *   the bottom of this file's own screen: a missing key draws its own name,
 *   loudly, rather than falling back and hiding.
 * - A sentence says **what happened and what to do**, in that order, and
 *   stops. Nobody reads a paragraph at arm's length with a parcel under one
 *   arm.
 * - No sentence here ever contains a phone number or a full name.
 */
window.CabinetText = (function () {
  "use strict";

  var WORDS = {
    vi: {
      "brand.sub": "Tủ gửi hàng thông minh",

      "drop.start": "Gửi hàng",
      "drop.start.sub": "Deliver a parcel",

      "number.ask": "Số điện thoại người nhận",
      "number.find": "Tìm",
      "number.finding": "Đang tìm…",
      "number.back": "Xoá số cuối",

      "confirm.ask": "Đúng người này không?",
      "confirm.no": "Không",
      "confirm.yes": "Đúng, mở hộp",

      /* Rung C. A question, not a statement - the server is guessing, and
       * the shipper is the only one who can say whether the guess is right. */
      "near.ask": "Có phải người này không?",
      "near.note": "Số bạn nhập lệch một vài chữ số so với số đã đặt hộp.",
      "near.no": "Không phải",
      "near.yes": "Đúng người này",

      /* Rung D. */
      "name.ask": "Nhập tên trên kiện hàng",
      "name.note": "Không tìm thấy số đó. Nhập đúng tên ghi trên nhãn.",
      "name.placeholder": "Ví dụ: Nguyen Van An",
      "name.check": "Kiểm tra tên",
      "name.checking": "Đang kiểm tra…",
      "name.back": "Nhập lại số",

      /* Rung E. The one sentence that sends somebody away, so it says
       * exactly where to go. */
      "abo.say":
        "Không tìm thấy người nhận. Hãy gọi cho họ và gửi kiện hàng tại ABO, " +
        "tiệm tạp hoá đối diện cổng sau.",

      "box.ask": "Bỏ kiện hàng vào hộp",
      "box.note": "Đóng cửa hộp khi xong.",
      "box.shut": "Đã đóng cửa",

      "trouble.again": "Bắt đầu lại",
      "trouble.refused": "Số đó bị từ chối. Kiểm tra lại và thử lần nữa.",
      "trouble.offline": "Không kết nối được máy chủ. Thử lại sau giây lát.",
      "trouble.full": "Tủ này đã đầy. Thử tủ khác.",
      "trouble.cabinet": "Tủ từ chối yêu cầu đó. Bắt đầu lại.",
      "trouble.noKey": "Tủ này chưa có khoá. Chưa nhận được hàng.",
      /* Unclear is not failure. A drop that timed out may have opened a
       * door, and telling the shipper it failed sends them away from a box
       * with their parcel already in it. */
      "trouble.unclear":
        "Chưa rõ hộp đã mở hay chưa. Nhìn vào tủ trước khi thử lại.",

      "qr.say": "Quét để nhận hàng",
      "qr.next": "Mã mới sau",
      "qr.cannotDraw": "Không vẽ được mã",
      "qr.waiting": "Đang chờ máy chủ",
      "qr.asking": "Đang xin mã từ máy chủ",
      "qr.noId": "Tủ này chưa có mã định danh",
      "qr.noKey": "Tủ này chưa có khoá hoặc chưa có địa chỉ máy chủ",
      "qr.badKey": "Máy chủ không nhận ra khoá của tủ này",
      "qr.refused": "Máy chủ từ chối: ",
      "foot.cabinet": "Tủ",
      "lang.other": "English",
    },

    en: {
      "brand.sub": "Smart parcel locker",

      "drop.start": "Deliver a parcel",
      "drop.start.sub": "Gửi hàng",

      "number.ask": "Receiver's phone number",
      "number.find": "Find",
      "number.finding": "Finding…",
      "number.back": "Delete the last digit",

      "confirm.ask": "Is this the right person?",
      "confirm.no": "No",
      "confirm.yes": "Yes, open a box",

      "near.ask": "Did you mean this person?",
      "near.note": "The number you typed is a digit or two off the one they booked with.",
      "near.no": "No, not them",
      "near.yes": "Yes, that is them",

      "name.ask": "Type the name on the parcel",
      "name.note": "That number found nobody. Type the name exactly as the label has it.",
      "name.placeholder": "For example: Nguyen Van An",
      "name.check": "Check the name",
      "name.checking": "Checking…",
      "name.back": "Type the number again",

      "abo.say":
        "Nobody here matches. Call the recipient, and leave the parcel at ABO, " +
        "the grocery store facing the campus back gate.",

      "box.ask": "Put the parcel in",
      "box.note": "Close the door when you are done.",
      "box.shut": "The door is shut",

      "trouble.again": "Start again",
      "trouble.refused": "That number was refused. Check it and try again.",
      "trouble.offline": "The server could not be reached. Try again in a moment.",
      "trouble.full": "Every box here is full. Try another cabinet.",
      "trouble.cabinet": "The cabinet refused that. Start again.",
      "trouble.noKey": "This cabinet has no key yet. It cannot take a parcel.",
      "trouble.unclear":
        "Not sure whether a box opened. Look at the cabinet before trying again.",

      "qr.say": "Scan to collect",
      "qr.next": "New code in",
      "qr.cannotDraw": "Cannot draw the code",
      "qr.waiting": "Waiting for the server",
      "qr.asking": "Asking the server for a code",
      "qr.noId": "This cabinet has no id yet",
      "qr.noKey": "This cabinet has no key or no server address yet",
      "qr.badKey": "This cabinet's key is not recognised",
      "qr.refused": "The server refused: ",
      "foot.cabinet": "Cabinet",
      "lang.other": "Tiếng Việt",
    },
  };

  /* Vietnamese, always, on every start. A cabinet has no user account and
   * nothing to remember somebody by, so there is no saved preference to
   * restore - and the next courier is a different person from the last one.
   * English is one tap away and lasts until the screen goes idle. */
  var current = "vi";

  function t(key) {
    var said = WORDS[current][key];
    if (said === undefined) {
      /* Loudly, rather than falling back to the other language. A missing
       * key that quietly draws English is a bug nobody ever notices. */
      return "!" + key + "!";
    }
    return said;
  }

  /* Fill every `data-t` on the page, and the placeholders beside them. */
  function apply(root) {
    var host = root || document;
    Array.prototype.forEach.call(host.querySelectorAll("[data-t]"), function (el) {
      el.textContent = t(el.getAttribute("data-t"));
    });
    Array.prototype.forEach.call(host.querySelectorAll("[data-t-placeholder]"), function (el) {
      el.setAttribute("placeholder", t(el.getAttribute("data-t-placeholder")));
    });
    Array.prototype.forEach.call(host.querySelectorAll("[data-t-label]"), function (el) {
      el.setAttribute("aria-label", t(el.getAttribute("data-t-label")));
    });
    document.documentElement.setAttribute("lang", current);
  }

  /* Anything that draws a sentence from code rather than from `data-t` -
   * the QR panel's status line, the keypad's Find key - registers here so
   * one tap really does change the whole screen. Without it those two would
   * stay in the old language until the next refresh, which is up to half a
   * minute of a screen that is half English and half Vietnamese. */
  var listeners = [];

  function onChange(fn) {
    listeners.push(fn);
  }

  function use(language) {
    if (!WORDS[language]) return;
    current = language;
    apply();
    listeners.forEach(function (fn) {
      fn(current);
    });
  }

  return {
    t: t,
    apply: apply,
    use: use,
    onChange: onChange,
    other: function () {
      return current === "vi" ? "en" : "vi";
    },
    current: function () {
      return current;
    },
    /* The key sets, for the check that both languages hold the same names. */
    keys: function (language) {
      return Object.keys(WORDS[language || current]);
    },
  };
})();
