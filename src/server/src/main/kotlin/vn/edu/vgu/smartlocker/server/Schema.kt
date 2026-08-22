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

    /**
     * Bring a database file up to date, whatever state it is in.
     *
     * **Every schema change from here on is a new entry in [MIGRATIONS], never
     * an edit to an existing one.** `CREATE TABLE IF NOT EXISTS` was enough
     * while every database was thrown away between runs; the moment one holds
     * a real parcel it is not. Adding a column to the list below without a
     * migration would leave the running database without it, and the server
     * would start cleanly and fail on the first query that used it.
     *
     * A migration already applied is skipped by number, so this is safe to run
     * on every start - which it is, from [Db].
     */
    fun migrate(db: Db) {
        val from = db.userVersion()
        MIGRATIONS.forEachIndexed { index, steps ->
            val version = index + 1
            if (version <= from) return@forEachIndexed
            db.transaction {
                steps.forEach(db::exec)
                db.setUserVersion(version)
            }
        }
    }

    /**
     * One entry per version, in order. Never reordered, never rewritten.
     *
     * Entry 1 is the baseline. It is every `CREATE TABLE IF NOT EXISTS` the
     * schema started with, so a database created before migrations existed -
     * which has the tables but `user_version = 0` - runs it as a no-op and
     * lands on version 1 with nothing changed.
     */
    private val MIGRATIONS: List<List<String>> by lazy {
        listOf(
            TABLES,

            // 2 — `opening_since`, so a parcel cannot be stranded mid-collect.
            //
            // A scan moves a parcel to `opening` and only a door-closed moves
            // it on. There is no sensor (ADR 0006), so that report is a tap
            // that may never come - and until this column existed there was
            // nothing to say how long a parcel had been waiting for it, so it
            // stayed `opening` forever and its owner was locked out.
            listOf(
                "ALTER TABLE parcels ADD COLUMN opening_since INTEGER",
                // Anything already stranded is stamped, so the timeout can
                // pick it up rather than leaving it stuck for good.
                "UPDATE parcels SET opening_since = 0 WHERE state = 'opening'",
            ),

            // 3 - clear out what the hashing key change made unreadable.
            //
            // Ids.hash became an HMAC under a key kept outside the database
            // file (see Pepper). Digests written before that cannot be matched
            // by anything a caller could type, and cannot be converted -
            // one-way is the point. What is left is rows that will never
            // match: a phone holding a token the server no longer recognises,
            // and one-time codes that expire in minutes anyway.
            //
            // Deleting them is the honest outcome and it is what happens on
            // its own otherwise, slowly, one confusing 401 at a time. Everyone
            // registers again once.
            //
            // Two things this cannot clean up. Pickup codes live on parcels
            // and deleting those would lose the parcel, so a code issued
            // before the change simply stops working and the receiver needs a
            // new one. Cabinet keys have to be rotated by hand -
            // `cabinet rotate <id>` - because the server never held anything
            // it could re-issue from.
            listOf(
                "DELETE FROM tokens",
                "DELETE FROM otp",
            ),

            // 4 - why a door was told to open, so nobody has to guess.
            //
            // BUG-009. Endpoint 12 needs `drop` or `collect` and endpoint 22
            // handed the hardware only `open`, so an ESP32 reporting a door
            // shut had no honest way to fill that in. Now the command says.
            //
            // Anything already queued is a drop: the collect path was not
            // reachable from the cabinet screen when this shipped.
            listOf(
                "ALTER TABLE commands ADD COLUMN purpose TEXT",
                "UPDATE commands SET purpose = 'drop' WHERE purpose IS NULL",
            ),

            // 5 - a password on an account, and the lockout that guards it.
            //
            // P2-09, ADR 0012. Three columns on `receivers` rather than a
            // table of its own: in this server the account IS the receiver,
            // and a parcel and a password that live in one row cannot
            // disagree about who somebody is. The reference server had its own
            // `accounts` table because it had no receivers to hang them on.
            //
            // Empty hash means no password, which is the state every existing
            // account is in and the state most will stay in - a password is
            // an extra way in, never the way in (there is no
            // register-with-a-password route).
            listOf(
                "ALTER TABLE receivers ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''",
                "ALTER TABLE receivers ADD COLUMN password_tries INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE receivers ADD COLUMN password_locked_until INTEGER NOT NULL DEFAULT 0",
            ),

            // 6 - the Google account tied to a receiver. Endpoint 19, P2-08.
            //
            // Keyed by Google's `sub`, never by the email address. A person
            // can change their Gmail address; `sub` stays the same for as
            // long as the account lives, so an email key would quietly
            // detach somebody from their own parcels.
            //
            // A column rather than a table: it is one value per receiver and
            // the link is the receiver's, not a thing of its own.
            //
            // UNIQUE, and that is the load-bearing part. Without it one
            // Google account could be linked to two phone numbers, and
            // signing in with Google would then reach whichever row the
            // database happened to return - somebody else's parcels. SQLite
            // lets any number of rows hold NULL in a unique column, which is
            // what makes NULL and not '' the right unlinked value.
            listOf(
                "ALTER TABLE receivers ADD COLUMN google_sub TEXT",
                "CREATE UNIQUE INDEX IF NOT EXISTS receivers_google ON receivers(google_sub)",
            ),
        )
    }

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
