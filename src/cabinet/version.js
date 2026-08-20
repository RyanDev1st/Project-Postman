/* The one place the cabinet screen's version is written.
 *
 * It is here on its own so a release changes one line in one file, and so
 * nothing has to guess the version from a build tool we have not chosen.
 *
 * Raise VERSION on every change that reaches a cabinet. The number on the
 * screen is how anybody standing at a cabinet says which build it is running
 * - which is the whole point of P1-03, and of the rollback page in P8-10.
 */
window.CABINET_BUILD = {
  VERSION: "0.3.0",

  /* Which cabinet this is. Placed on the device at setup, never built in -
   * the same rule as the cabinet key. Until a real cabinet exists there is
   * nothing to place it, so it reads as unset rather than pretending. */
  CABINET_ID: null,
};
