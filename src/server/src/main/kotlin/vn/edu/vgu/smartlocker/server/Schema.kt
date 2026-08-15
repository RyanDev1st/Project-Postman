package vn.edu.vgu.smartlocker.server

/**
 * Every table, in one place.
 *
 * **A parcel belongs to a receiver.** That is the line the Server team's schema
 * did not have, and without it no query can answer *is this parcel yours* -
 * which is the only question the pickup flow exists to ask. See ADR 0019.
 *
 * Every join is a real `REFERENCES`, and `PRAGMA foreign_keys=ON` is set in
 * [Db], so a parcel cannot point at a cabinet that does not exist.
 *
 * Times are **epoch milliseconds, UTC**, as `INTEGER`. The contract says ISO
 * 8601 on the wire and that is where it is formatted - storing text here would
 * mean comparing dates as strings, and the collect path compares a deadline on
 * every call.
 *
 * A box number is `TEXT`, never `INTEGER`. The doors are labelled `01` to `20`
 * and an int drops the leading zero, printing `4` on a door that says `04`.
 */
object Schema {

    fun create(db: Db) = TABLES.forEach(db::exec)

    private val TABLES = listOf(

        // --- Who ----------------------------------------------------------

        """CREATE TABLE IF NOT EXISTS receivers (
             id           TEXT PRIMARY KEY,
             phone        TEXT NOT NULL UNIQUE,
             full_name    TEXT NOT NULL DEFAULT '',
             created_at   INTEGER NOT NULL
           )""",

        // A token is stored hashed. A leaked database file is then a list of
        // useless hashes rather than a list of working logins.
        """CREATE TABLE IF NOT EXISTS tokens (
             token_hash   TEXT PRIMARY KEY,
             receiver_id  TEXT NOT NULL REFERENCES receivers(id) ON DELETE CASCADE,
             expires_at   INTEGER NOT NULL
           )""",

        // One row per phone number waiting for a code. Hashed for the same
        // reason as a token: it is a credential until it is used.
        """CREATE TABLE IF NOT EXISTS otp (
             phone        TEXT PRIMARY KEY,
             code_hash    TEXT NOT NULL,
             expires_at   INTEGER NOT NULL,
             tries_left   INTEGER NOT NULL,
             last_sent_at INTEGER NOT NULL
           )""",

        """CREATE TABLE IF NOT EXISTS devices (
             receiver_id  TEXT NOT NULL REFERENCES receivers(id) ON DELETE CASCADE,
             device_id    TEXT NOT NULL,
             registered_at INTEGER NOT NULL,
             PRIMARY KEY (receiver_id, device_id)
           )""",

        // --- Where --------------------------------------------------------

        // The key is hashed, so this file never holds a working cabinet key.
        """CREATE TABLE IF NOT EXISTS cabinets (
             id           TEXT PRIMARY KEY,
             name         TEXT NOT NULL,
             key_hash     TEXT NOT NULL,
             created_at   INTEGER NOT NULL
           )""",

        // `state` is one of: free, taken, faulty.
        """CREATE TABLE IF NOT EXISTS boxes (
             cabinet_id   TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
             number       TEXT NOT NULL,
             size         TEXT NOT NULL DEFAULT 'medium',
             state        TEXT NOT NULL DEFAULT 'free',
             PRIMARY KEY (cabinet_id, number)
           )""",

        // --- What ---------------------------------------------------------

        // `receiver_id` is the whole point of this file.
        //
        // `state` is one of: waiting, collected, gone.
        // `pickup_code_hash` is the typed backup code - path D2, never the QR.
        """CREATE TABLE IF NOT EXISTS parcels (
             id                TEXT PRIMARY KEY,
             receiver_id       TEXT NOT NULL REFERENCES receivers(id) ON DELETE CASCADE,
             cabinet_id        TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
             box_number        TEXT NOT NULL,
             state             TEXT NOT NULL DEFAULT 'waiting',
             arrived_at        INTEGER NOT NULL,
             collected_at      INTEGER,
             pickup_code_hash  TEXT,
             pickup_code_expires_at INTEGER,
             FOREIGN KEY (cabinet_id, box_number) REFERENCES boxes(cabinet_id, number)
           )""",

        "CREATE INDEX IF NOT EXISTS parcels_by_owner ON parcels(receiver_id, cabinet_id, state)",

        // --- The QR session -----------------------------------------------

        // The server remembers what it handed out. A code that is not a row in
        // here was not issued by this server, whatever it looks like, and that
        // is the first rule in "The scanned code - what protects it".
        """CREATE TABLE IF NOT EXISTS sessions (
             code         TEXT PRIMARY KEY,
             cabinet_id   TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
             issued_at    INTEGER NOT NULL,
             expires_at   INTEGER NOT NULL
           )""",

        // One open per session code, per parcel. The primary key is the rule:
        // a replayed request hits it and is refused by the database itself,
        // not by a check somebody has to remember to write.
        """CREATE TABLE IF NOT EXISTS session_use (
             code         TEXT NOT NULL,
             parcel_id    TEXT NOT NULL,
             used_at      INTEGER NOT NULL,
             PRIMARY KEY (code, parcel_id)
           )""",

        // --- The lockout counter, on disk ----------------------------------

        """CREATE TABLE IF NOT EXISTS lockouts (
             cabinet_id   TEXT NOT NULL,
             box_number   TEXT NOT NULL,
             wrong_tries  INTEGER NOT NULL DEFAULT 0,
             locked_until INTEGER NOT NULL DEFAULT 0,
             PRIMARY KEY (cabinet_id, box_number)
           )""",

        // --- What the hardware is told -------------------------------------

        // The ESP32 cannot be dialled - it takes a DHCP address and has no
        // name - so it asks, and is never told. A row here is a door waiting
        // to be opened. `taken_at` is stamped when the hardware collects it,
        // so a retrying ESP32 cannot open the same door twice. See ADR 0020.
        """CREATE TABLE IF NOT EXISTS commands (
             id           TEXT PRIMARY KEY,
             cabinet_id   TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
             box_number   TEXT NOT NULL,
             action       TEXT NOT NULL,
             created_at   INTEGER NOT NULL,
             taken_at     INTEGER,
             done_at      INTEGER,
             result       TEXT
           )""",

        "CREATE INDEX IF NOT EXISTS commands_waiting ON commands(cabinet_id, taken_at)",

        // --- What happened -------------------------------------------------

        // The log. When a locker opens, this is the evidence - not the screen.
        // Nothing is ever updated or deleted here.
        """CREATE TABLE IF NOT EXISTS events (
             id           TEXT PRIMARY KEY,
             at           INTEGER NOT NULL,
             receiver_id  TEXT,
             cabinet_id   TEXT NOT NULL,
             box_number   TEXT NOT NULL,
             parcel_id    TEXT,
             action       TEXT NOT NULL,
             detail       TEXT NOT NULL DEFAULT ''
           )""",

        "CREATE INDEX IF NOT EXISTS events_by_receiver ON events(receiver_id, at)",
    )
}
