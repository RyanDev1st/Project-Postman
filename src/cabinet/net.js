/* The cabinet's one door. Every network call it makes goes through here.
 *
 * The same rule as the app's net/Http.kt - rule 1 in architecture.md - and
 * the same checker proves it: scripts/checknet.py fails if any other file in
 * src/cabinet/ calls fetch, opens a WebSocket, or reaches the network at all.
 *
 * The cabinet is a PUBLIC TERMINAL. Anyone can walk up to it, and on a
 * touchscreen in a lobby there is no logged-in person to protect. That
 * changes what this file must refuse, compared to the app's door:
 *
 *   - It signs in as a DEVICE, with a cabinet key, never as a person.
 *   - The key is PLACED ON THE DEVICE at setup and is never in this repo.
 *     Task P0-05 decides how it gets there; until then it is a test key we
 *     issue ourselves, and it still is not committed.
 *   - It never logs the key, a phone number, or a body. A cabinet screen is
 *     in a public place and its console may be open on a service laptop.
 *
 * Three things it will not do, same as the app:
 *
 *   1. Plain HTTP. HTTPS is fixed by api-contract.md, not a setting.
 *   2. Retry. An open-door call moves real metal, and a retry can open a
 *      door nobody is standing at. Rule 3.
 *   3. Report an unknown as a success. Rule 4: unclear is not success.
 */
window.CabinetNet = (function () {
  "use strict";

  var CONNECT_MS = 10000;

  /* Where the key lives at runtime.
   *
   * Read from the device's own config - the same place CABINET_ID comes
   * from - and never from this file. A key written here would be in git, in
   * every clone, and in the browser's view-source on a public screen. */
  function key() {
    return (window.CABINET_CONFIG || {}).KEY || null;
  }

  function baseUrl() {
    return (window.CABINET_CONFIG || {}).SERVER_BASE_URL || "";
  }

  /* The three outcomes. Same shape as the app's Answer, on purpose: the two
   * front-ends should describe the same event the same way, or the two teams
   * end up debugging two vocabularies. */
  function ok(value) {
    return { state: "ok", value: value };
  }

  function refused(code) {
    return { state: "refused", code: code };
  }

  /* We do not know what happened. Never draw this as success. */
  function unclear(why) {
    return { state: "unclear", why: why };
  }

  /* One request, one answer.
   *
   * Returns a promise that always RESOLVES - to one of the three states
   * above - and never rejects. A rejected promise is how a missing catch
   * turns a network blip into a blank screen on a cabinet nobody is
   * watching. The caller must handle three cases, so it is made to.
   */
  function call(path, options) {
    options = options || {};

    var url = baseUrl();
    if (!url) {
      return Promise.resolve(unclear("no server address is set"));
    }

    var full;
    try {
      full = new URL(path, url);
    } catch (e) {
      return Promise.resolve(unclear("bad address"));
    }

    /* HTTPS or nothing, checked on the parsed URL so a relative path that
     * resolves onto an http base is caught too. */
    if (full.protocol !== "https:") {
      return Promise.resolve(unclear("refused to send over " + full.protocol));
    }

    var cabinetKey = key();
    if (!cabinetKey) {
      return Promise.resolve(unclear("this cabinet has no key yet"));
    }

    var headers = {
      Accept: "application/json",
      "X-Cabinet-Key": cabinetKey,
    };
    if (options.body) {
      headers["Content-Type"] = "application/json; charset=utf-8";
    }

    /* AbortController, because fetch has no timeout of its own and a request
     * that never settles leaves the screen showing a spinner forever. */
    var stop = new AbortController();
    var timer = setTimeout(function () {
      stop.abort();
    }, options.timeoutMs || CONNECT_MS);

    return fetch(full.toString(), {
      method: options.method || "GET",
      headers: headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
      signal: stop.signal,
      /* No cookies, ever. The cabinet is identified by its key alone, and a
       * public terminal that carries session cookies is one that remembers
       * the last person who stood at it. */
      credentials: "omit",
      /* Do not follow redirects. A redirect can move an https request to
       * http, or to another host. */
      redirect: "error",
      cache: "no-store",
    })
      .then(function (response) {
        return response.text().then(function (text) {
          return interpret(response.status, text);
        });
      })
      .catch(function (e) {
        /* Aborted, offline, DNS, TLS - from here they are the same thing:
         * the request may well have arrived, and we cannot tell. */
        return unclear(
          e && e.name === "AbortError"
            ? "the server did not answer in time"
            : "could not reach the server"
        );
      })
      .then(function (answer) {
        clearTimeout(timer);
        return answer;
      });
  }

  /* Status code plus body into one of the three states.
   *
   * The refusal comes from the body's `code` field, never guessed from the
   * status. That vocabulary is the server's - the table in api-contract.md -
   * and inventing a meaning for a 404 here is how two teams end up
   * disagreeing about what happened. */
  function interpret(status, text) {
    var json = {};
    if (text) {
      try {
        json = JSON.parse(text);
      } catch (e) {
        /* A 200 we cannot read is not a success. */
        return unclear("the server's answer could not be read");
      }
    }

    if (status >= 200 && status < 300) {
      return ok(json);
    }
    if (json && json.code) {
      return refused(String(json.code));
    }
    return unclear("the server answered " + status);
  }

  return {
    call: call,

    /* Whether this cabinet has been given its key yet. The screen uses it to
     * say so plainly rather than failing on the first call. */
    ready: function () {
      return Boolean(key() && baseUrl());
    },
  };
})();
