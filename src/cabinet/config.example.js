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
 * WHERE A REAL KEY COMES FROM
 *
 * Our own server issues it, once, when the cabinet is created:
 *
 *     ./gradlew :server:run --args="cabinet add vgu-back-gate 'VGU Back Gate' 20"
 *
 * That prints the key one time. The server keeps only a hash and cannot show
 * it again. If it is lost, or ends up somewhere it should not be, replace it -
 * the old one stops working the moment you do:
 *
 *     ./gradlew :server:run --args="cabinet rotate vgu-back-gate"
 *
 * Task P0-05 is what a person physically does at a cabinet to get it onto the
 * device. That is the hardware team's half; the issuing half is done.
 */
window.CABINET_CONFIG = {
  /* Which cabinet this is. Appears in the server log so a request can be
   * traced to one machine. Not a secret.
   *
   * It must be an id the server knows - `cabinet list` says which. A screen
   * whose key the server does not recognise draws no code and says so. */
  CABINET_ID: "vgu-test-01",

  /* HTTPS only. api-contract.md makes that fixed, not a setting, and
   * net.js refuses anything else.
   *
   * The server prints every address it can be reached on when it starts. Use
   * the LAN one, not localhost, unless the screen is on the same machine. The
   * certificate has to be trusted by this browser first - see
   * src/server/README.md. */
  SERVER_BASE_URL: "https://example.invalid",

  /* How long a QR session code is good for, and how often the screen draws
   * a new one. Both are guesses from P0-07 and both live in
   * `config/settings.json` as the source of truth - these are the same
   * numbers, repeated so one cabinet can be changed without a release.
   *
   * QR_SESSION_SECONDS is now the SERVER'S number - it issues the code and
   * decides how long it lives, and sends that back with it. The value here is
   * only what the countdown falls back to before the first answer arrives.
   * Raising it here changes nothing the server does.
   *
   * QR_REFRESH_SECONDS is still this screen's: how often it asks for a new
   * code. Leave them out and qr.js uses 60 and 30. */
  QR_SESSION_SECONDS: 60,
  QR_REFRESH_SECONDS: 30,

  /* The cabinet key. Sent on every call, as X-Cabinet-Key.
   *
   * The value below is deliberately not a key and will not work anywhere.
   * If a real one ever appears in this file, it has been committed - rotate
   * it first, then remove it. Removing it alone does nothing: it stays in
   * the git history and in every clone. */
  KEY: "PLACEHOLDER-NOT-A-REAL-KEY-RUN-cabinet-add",
};
