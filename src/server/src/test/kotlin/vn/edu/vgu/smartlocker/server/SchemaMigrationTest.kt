package vn.edu.vgu.smartlocker.server

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Migration 7, on a database that already holds somebody's parcel.
 *
 * Every other migration in this file's history adds a column, which SQLite
 * does in place and cannot get wrong. This one **rebuilds `receivers`** -
 * copy out, drop, rename in - because `phone` has to stop being `NOT NULL`
 * and SQLite cannot relax a column constraint any other way. ADR 0026, task
 * P2-12.
 *
 * A `DROP TABLE` with `PRAGMA foreign_keys=ON` performs an implicit
 * `DELETE FROM` first, and that implicit delete **fires `ON DELETE CASCADE`**.
 * Four tables hang off `receivers` that way. So the wrong version of this
 * migration wipes every token, device and parcel in the file, inside a
 * transaction that then commits cleanly and a server that then starts
 * cleanly. Nothing would say anything until a student opened the app and
 * found their parcel gone.
 *
 * [`the rebuild keeps what hangs off receivers`] is the test that catches
 * that, and it is the reason this file exists. It builds a database in the
 * shape version 6 shipped, fills it, and migrates it for real.
 */
class SchemaMigrationTest {

    private lateinit var file: File

    /** The newest migration. Raise it when one is added, and say why here. */
    private val newest = 8

    @BeforeTest
    fun open() {
        val key = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        key.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }))
        Pepper.load(key)
        file = File.createTempFile("migration", ".db").apply { delete() }
    }

    @AfterTest
    fun clean() {
        file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
    }

    @Test
    fun `a fresh database lands on the newest version`() {
        Db(file).use { assertEquals(newest, it.userVersion()) }
    }

    /**
     * The one that says a restart is free. A migration already applied is
     * skipped by number, so starting the server twice is not a schema change
     * twice - and `ALTER TABLE ... ADD COLUMN` would throw the second time.
     */
    @Test
    fun `opening the same file again changes nothing`() {
        Db(file).use { db ->
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                "r1", "+84912340001", "Minh", 1_700_000_000_000L,
            )
        }
        Db(file).use { db ->
            assertEquals(newest, db.userVersion())
            assertEquals(1, db.row("SELECT COUNT(*) AS n FROM receivers") { it.getInt("n") })
        }
    }

    // --- What migration 7 changed ------------------------------------------

    /**
     * An account with no number is the normal state of a brand-new Google
     * sign-in, and there can be any number of them at once. That is why the
     * absent value is NULL and not `''`: SQLite allows many NULLs in a unique
     * column and exactly one `''`, so with an empty string the **second**
     * account ever created would fail to insert.
     */
    @Test
    fun `many accounts may have no number at all`() {
        Db(file).use { db ->
            repeat(3) { n ->
                db.exec(
                    "INSERT INTO receivers (id, phone, full_name, created_at) " +
                        "VALUES (?, NULL, ?, ?)",
                    "r$n", "Nobody $n", 1_700_000_000_000L,
                )
            }
            assertEquals(
                3,
                db.row("SELECT COUNT(*) AS n FROM receivers WHERE phone IS NULL") { it.getInt("n") },
            )
        }
    }

    /**
     * Unique **when set** is the other half. Two accounts holding one number
     * would mean the cabinet's lookup picks whichever row comes back first,
     * and that is a coin toss between two people's parcels.
     */
    @Test
    fun `two accounts cannot hold the same number`() {
        Db(file).use { db ->
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                "r1", "+84912340001", "Minh", 1_700_000_000_000L,
            )
            assertFailsWith<SQLException> {
                db.exec(
                    "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                    "r2", "+84912340001", "An", 1_700_000_000_000L,
                )
            }
        }
    }

    /** One live booking to an account, kept by the database and not by a rule. */
    @Test
    fun `an account may hold only one booking, and a door only one booker`() {
        Db(file).use { db ->
            seedCabinet(db)
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                "r1", "+84912340001", "Minh", 1_700_000_000_000L,
            )
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                "r2", "+84912340002", "An", 1_700_000_000_000L,
            )
            db.exec(booking, "b1", "r1", "cab", "07", 0L, 1L)

            // The same person, a second door.
            assertFailsWith<SQLException> { db.exec(booking, "b2", "r1", "cab", "08", 0L, 1L) }
            // A different person, the same door.
            assertFailsWith<SQLException> { db.exec(booking, "b3", "r2", "cab", "07", 0L, 1L) }
            // A different person, a different door. Fine.
            db.exec(booking, "b4", "r2", "cab", "08", 0L, 1L)
        }
    }

    /** A booking cannot hold a door at a cabinet that does not exist. */
    @Test
    fun `a booking must name a real cabinet`() {
        Db(file).use { db ->
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                "r1", "+84912340001", "Minh", 1_700_000_000_000L,
            )
            assertFailsWith<SQLException> { db.exec(booking, "b1", "r1", "no-such-cabinet", "07", 0L, 1L) }
        }
    }

    // --- The one that pays for this file -----------------------------------

    /**
     * **Migration 7 must not take anything with it when it drops `receivers`.**
     *
     * Built as version 6 shipped, filled with a token, a device, a parcel and
     * a password hash, and then migrated for real. Every count below was 1
     * before, and a cascading drop would make every one of them 0 while the
     * server started without a word.
     *
     * The password columns are read back too, because the rebuild copies them
     * across by name: a column left out of that `INSERT ... SELECT` silently
     * resets everybody's password and their lockout counter.
     */
    @Test
    fun `the rebuild keeps what hangs off receivers`() {
        buildVersionSix()

        Db(file).use { db ->
            assertEquals(newest, db.userVersion())

            assertEquals(1, db.row("SELECT COUNT(*) AS n FROM tokens") { it.getInt("n") })
            assertEquals(1, db.row("SELECT COUNT(*) AS n FROM devices") { it.getInt("n") })
            assertEquals(1, db.row("SELECT COUNT(*) AS n FROM parcels") { it.getInt("n") })
            assertEquals(1, db.row("SELECT COUNT(*) AS n FROM receivers") { it.getInt("n") })

            val kept = db.row(
                "SELECT phone, full_name, password_hash, password_tries, google_sub " +
                    "FROM receivers WHERE id = 'r1'",
            ) {
                listOf(
                    it.str("phone"),
                    it.str("full_name"),
                    it.str("password_hash"),
                    it.getInt("password_tries").toString(),
                    it.str("google_sub"),
                )
            }
            assertEquals(
                listOf("+84912340001", "Minh", "argon2id\$19456\$2\$1\$abc", "3", "google-sub-1"),
                kept,
            )

            // The parcel still points at its owner, not at a hole.
            assertEquals(
                "r1",
                db.row("SELECT receiver_id FROM parcels WHERE id = 'p1'") { it.str("receiver_id") },
            )
            assertTrue(db.rows("PRAGMA foreign_key_check") { it.str("table") }.isEmpty())
        }
    }

    /**
     * And the cascade still works afterwards. The rebuild recreates the
     * table, so a `REFERENCES` clause dropped from the new definition would
     * leave orphans behind on the next delete rather than failing loudly.
     */
    @Test
    fun `deleting a receiver still takes its own rows with it`() {
        buildVersionSix()
        Db(file).use { db ->
            db.exec("DELETE FROM receivers WHERE id = 'r1'")
            assertEquals(0, db.row("SELECT COUNT(*) AS n FROM tokens") { it.getInt("n") })
            assertEquals(0, db.row("SELECT COUNT(*) AS n FROM parcels") { it.getInt("n") })
            assertNull(db.row("SELECT id FROM receivers WHERE id = 'r1'") { it.str("id") })
        }
    }

    // --- Migration 8, and the damage it clears. BUG-030 --------------------

    /**
     * **A token whose owner is gone still signs somebody in**, and that is
     * why these are deleted rather than left as dead weight.
     * `Tokens.receiverFor` reads `receiver_id` straight off the row, so the
     * holder gets a session belonging to an account that does not exist.
     *
     * The damage is reproduced the way it really happened: a delete over a
     * connection that never turned foreign keys on, which is every Python
     * script and every `sqlite3` prompt, because SQLite defaults them OFF per
     * connection. `scripts/stress.py` did exactly this 180 times.
     */
    @Test
    fun `a token whose owner is gone is thrown away`() {
        buildVersionSix()
        deleteTheReceiverTheCarelessWay()

        // The bug, before the fix: the receiver is gone and the token is not.
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            assertEquals(0, c.count("SELECT COUNT(*) AS n FROM receivers"))
            assertEquals(1, c.count("SELECT COUNT(*) AS n FROM tokens"))
        }

        Db(file).use { db ->
            assertEquals(newest, db.userVersion())
            assertEquals(0, db.row("SELECT COUNT(*) AS n FROM tokens") { it.getInt("n") })
            assertTrue(db.rows("PRAGMA foreign_key_check") { it.str("table") }.isEmpty())
        }
    }

    /**
     * Damage a migration did not cause does not stop the server.
     *
     * The rule is *worse than it started*, not *not clean*. The live file was
     * already carrying orphans on 2026-09-03, and refusing to start on those
     * would turn an old data wart into an outage - the worse of the two
     * failures. Proven with a parcel, which no migration sweeps, so it is
     * still an orphan on the far side.
     */
    @Test
    fun `damage that was already there does not stop the server`() {
        buildVersionSix()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            // Foreign keys still off, so the parcel is left pointing at a box
            // that is gone. This is the shape of an old wound, not a new one.
            c.run("DELETE FROM boxes WHERE cabinet_id = 'cab' AND number = '07'")
        }

        Db(file).use { db ->
            assertEquals(newest, db.userVersion())
            assertEquals(1, db.row("SELECT COUNT(*) AS n FROM parcels") { it.getInt("n") })
            assertEquals(
                listOf("parcels"),
                db.rows("PRAGMA foreign_key_check") { it.str("table") }.distinct(),
            )
        }
    }

    // --- Fixtures ----------------------------------------------------------

    private val booking =
        "INSERT INTO bookings (id, receiver_id, cabinet_id, box_number, created_at, expires_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)"

    private fun seedCabinet(db: Db) {
        db.exec(
            "INSERT INTO cabinets (id, name, key_hash, created_at) VALUES (?, ?, ?, ?)",
            "cab", "Test cabinet", "hash", 0L,
        )
        listOf("07", "08").forEach {
            db.exec(
                "INSERT INTO boxes (cabinet_id, number, size, state) VALUES (?, ?, 'medium', 'free')",
                "cab", it,
            )
        }
    }

    /**
     * A database in the shape version 6 shipped, with real rows in it.
     *
     * Written out by hand rather than driven through `Schema`, because the
     * point is to reproduce a file that exists on disk today - the shape a
     * running server left behind before migration 7 was written. Driving it
     * through the current code would prove only that the current code agrees
     * with itself.
     *
     * Foreign keys are ON while it is filled, exactly as [Db] sets them, so
     * the rows here are as real as the ones a server writes.
     */
    private fun buildVersionSix() {
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            c.autoCommit = true
            c.run("PRAGMA journal_mode=WAL")
            c.run("PRAGMA foreign_keys=ON")

            c.run(
                """CREATE TABLE receivers (
                     id TEXT PRIMARY KEY,
                     phone TEXT NOT NULL UNIQUE,
                     full_name TEXT NOT NULL DEFAULT '',
                     created_at INTEGER NOT NULL,
                     password_hash TEXT NOT NULL DEFAULT '',
                     password_tries INTEGER NOT NULL DEFAULT 0,
                     password_locked_until INTEGER NOT NULL DEFAULT 0,
                     google_sub TEXT
                   )""",
            )
            c.run("CREATE UNIQUE INDEX receivers_google ON receivers(google_sub)")
            c.run(
                """CREATE TABLE tokens (
                     token_hash TEXT PRIMARY KEY,
                     receiver_id TEXT NOT NULL REFERENCES receivers(id) ON DELETE CASCADE,
                     expires_at INTEGER NOT NULL
                   )""",
            )
            c.run(
                """CREATE TABLE otp (
                     phone TEXT PRIMARY KEY, code_hash TEXT NOT NULL,
                     expires_at INTEGER NOT NULL, tries_left INTEGER NOT NULL,
                     last_sent_at INTEGER NOT NULL
                   )""",
            )
            c.run(
                """CREATE TABLE devices (
                     receiver_id TEXT NOT NULL REFERENCES receivers(id) ON DELETE CASCADE,
                     device_id TEXT NOT NULL, registered_at INTEGER NOT NULL,
                     PRIMARY KEY (receiver_id, device_id)
                   )""",
            )
            c.run(
                """CREATE TABLE cabinets (
                     id TEXT PRIMARY KEY, name TEXT NOT NULL,
                     key_hash TEXT NOT NULL, created_at INTEGER NOT NULL
                   )""",
            )
            c.run(
                """CREATE TABLE boxes (
                     cabinet_id TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
                     number TEXT NOT NULL, size TEXT NOT NULL DEFAULT 'medium',
                     state TEXT NOT NULL DEFAULT 'free',
                     PRIMARY KEY (cabinet_id, number)
                   )""",
            )
            c.run(
                """CREATE TABLE parcels (
                     id TEXT PRIMARY KEY,
                     receiver_id TEXT NOT NULL REFERENCES receivers(id) ON DELETE CASCADE,
                     cabinet_id TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
                     box_number TEXT NOT NULL, state TEXT NOT NULL DEFAULT 'waiting',
                     arrived_at INTEGER NOT NULL, collected_at INTEGER,
                     pickup_code_hash TEXT, pickup_code_expires_at INTEGER,
                     opening_since INTEGER,
                     FOREIGN KEY (cabinet_id, box_number) REFERENCES boxes(cabinet_id, number)
                   )""",
            )
            c.run(
                """CREATE TABLE sessions (
                     code TEXT PRIMARY KEY,
                     cabinet_id TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
                     issued_at INTEGER NOT NULL, expires_at INTEGER NOT NULL
                   )""",
            )
            c.run(
                """CREATE TABLE session_use (
                     code TEXT NOT NULL, parcel_id TEXT NOT NULL, used_at INTEGER NOT NULL,
                     PRIMARY KEY (code, parcel_id)
                   )""",
            )
            c.run(
                """CREATE TABLE lockouts (
                     cabinet_id TEXT NOT NULL, box_number TEXT NOT NULL,
                     wrong_tries INTEGER NOT NULL DEFAULT 0,
                     locked_until INTEGER NOT NULL DEFAULT 0,
                     PRIMARY KEY (cabinet_id, box_number)
                   )""",
            )
            c.run(
                """CREATE TABLE commands (
                     id TEXT PRIMARY KEY,
                     cabinet_id TEXT NOT NULL REFERENCES cabinets(id) ON DELETE CASCADE,
                     box_number TEXT NOT NULL, action TEXT NOT NULL,
                     created_at INTEGER NOT NULL, taken_at INTEGER, done_at INTEGER,
                     result TEXT, purpose TEXT
                   )""",
            )
            c.run(
                """CREATE TABLE events (
                     id TEXT PRIMARY KEY, at INTEGER NOT NULL, receiver_id TEXT,
                     cabinet_id TEXT NOT NULL, box_number TEXT NOT NULL, parcel_id TEXT,
                     action TEXT NOT NULL, detail TEXT NOT NULL DEFAULT ''
                   )""",
            )

            // A receiver with everything hanging off them, so a cascade has
            // something to destroy.
            c.run(
                "INSERT INTO receivers (id, phone, full_name, created_at, " +
                    "password_hash, password_tries, password_locked_until, google_sub) " +
                    "VALUES ('r1', '+84912340001', 'Minh', 1700000000000, " +
                    "'argon2id\$19456\$2\$1\$abc', 3, 0, 'google-sub-1')",
            )
            c.run("INSERT INTO tokens VALUES ('token-hash-1', 'r1', 9999999999999)")
            c.run("INSERT INTO devices VALUES ('r1', 'device-1', 1700000000000)")
            c.run("INSERT INTO cabinets VALUES ('cab', 'Test cabinet', 'hash', 1700000000000)")
            c.run("INSERT INTO boxes VALUES ('cab', '07', 'medium', 'taken')")
            c.run(
                "INSERT INTO parcels (id, receiver_id, cabinet_id, box_number, state, arrived_at) " +
                    "VALUES ('p1', 'r1', 'cab', '07', 'waiting', 1700000000000)",
            )
            c.run("PRAGMA user_version = 6")
        }
    }

    /**
     * Delete the receiver over a connection with foreign keys off, which is
     * what every Python script and every `sqlite3` prompt does unless it is
     * told otherwise. The cascade never fires and the token is left behind.
     */
    private fun deleteTheReceiverTheCarelessWay() {
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            // A load-test account, as `stress.py` leaves one: it registered
            // and did nothing else, so a token is all it has.
            c.run("DELETE FROM parcels WHERE receiver_id = 'r1'")
            c.run("DELETE FROM devices WHERE receiver_id = 'r1'")
            c.run("DELETE FROM receivers WHERE id = 'r1'")
        }
    }

    private fun Connection.run(sql: String) = createStatement().use { it.execute(sql) }

    private fun Connection.count(sql: String): Int =
        createStatement().use { st ->
            st.executeQuery(sql).use {
                it.next()
                it.getInt("n")
            }
        }
}
