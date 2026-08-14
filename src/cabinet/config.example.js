/* What a real cabinet's config.js looks like. THIS FILE HAS NO REAL VALUES.
 *
 * Copy it to `config.js` on the cabinet itself and fill it in there.
 * `config.js` is git-ignored and must never be committed, because it holds
 * the cabinet key.
 *
 *     cp config.example.js config.js      # on the device, not in the repo
 *
 * Why the key is not in the repo, spelled out once:
 *
 *   - Every clone of this repo would have it, including on laptops that
 *     leave campus.
 *   - The cabinet screen is a web page. Anyone standing at a public terminal
 *     can open view-source, and a key in a committed file is a key on that
 *     screen.
 *   - One key per cabinet - ADR 0004 - so a shared committed key would
 *     defeat the one property that stops a single loss spreading.
 *
 * Task P0-05 decides how a real key reaches a cabinet, and how it is
 * replaced if one is stolen. It waits on the Server team. Until then this is
 * a test key we issue ourselves, and it is still never committed.
 */
window.CABINET_CONFIG = {
  /* Which cabinet this is. Appears in the server log so a request can be
   * traced to one machine. Not a secret. */
  CABINET_ID: "vgu-test-01",

  /* HTTPS only. api-contract.md makes that fixed, not a setting, and
   * net.js refuses anything else. */
  SERVER_BASE_URL: "https://example.invalid",

  /* How long a QR session code is good for, and how often the screen draws
   * a new one. Both are guesses from P0-07 and both live in
   * `config/settings.json` as the source of truth - these are the same
   * numbers, repeated so one cabinet can be changed without a release.
   *
   * Raise QR_SESSION_SECONDS if people find a code expires while they are
   * still walking up. It is safe to: the code says which cabinet and when,
   * and nothing else, so a longer life is not the risk it would be for a
   * typed code. Nothing else has to change.
   *
   * Leave them out and qr.js uses 60 and 30. */
  QR_SESSION_SECONDS: 60,
  QR_REFRESH_SECONDS: 30,

  /* The cabinet key. Sent on every call, as X-Cabinet-Key.
   *
   * The value below is deliberately not a key and will not work anywhere.
   * If a real one ever appears in this file, it has been committed - rotate
   * it first, then remove it. Removing it alone does nothing: it stays in
   * the git history and in every clone. */
  KEY: "PLACEHOLDER-NOT-A-REAL-KEY-SEE-P0-05",
};
