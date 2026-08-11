package otp.password

import java.sql.Connection
import java.sql.DriverManager

/**
 * Password hashes and lockout counters, on disk.
 *
 * On disk and not in memory, because of one rule: **the lockout counter has
 * to survive a power cut.** A counter held in memory resets when the process
 * dies, so an attacker who can crash or simply outwait a restart gets an
 * unlimited number of guesses. Everything else in this server is in memory
 * on purpose; these two columns are the exception, and the reason is in
 * [ADR 0012](../../../../../../docs/adr/0012-passwords-on-a-phone-account.md).
 *
 * SQLite rather than a file we write ourselves: a half-written file after a
 * power cut is exactly the failure the rule names, and a database that has
 * spent twenty years on that problem will beat anything written here today.
 *
 * The numbers mirror `wrong_tries_before_lock` in `config/settings.json`.
 * They are repeated rather than read, the same way AuthStore repeats the
 * code lifetime — this server does not load that file.
 */
class Accounts(
    dbPath: String,
    private val now: () -> Long = System::currentTimeMillis,
) : AutoCloseable {

    private data class Row(val hash: String, val wrongTries: Int, val lockedUntil: Long)

    private val db: Connection =
        DriverManager.getConnection("jdbc:sqlite:$dbPath").apply {
            createStatement().use { s ->
                // FULL, not NORMAL. NORMAL lets the last few commits sit in
                // the operating system's buffers, where a power cut loses
                // them - and losing them means losing the lockout counter.
                // It costs a disk flush per login, which is not a hot path.
                s.execute("PRAGMA journal_mode=WAL")
                s.execute("PRAGMA synchronous=FULL")
                s.execute(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                      phone        TEXT PRIMARY KEY,
                      hash         TEXT    NOT NULL,
                      wrong_tries  INTEGER NOT NULL DEFAULT 0,
                      locked_until INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

    /**
     * Set or replace the password on an account the caller has already
     * proved. Returns a refusal code, or null when it is set. Setting one
     * clears any lockout: the person just proved themselves another way.
     */
    fun setPassword(phoneE164: String, password: String): String? {
        Passwords.refuse(password)?.let { return it }
        db.prepareStatement(
            """
            INSERT INTO accounts (phone, hash, wrong_tries, locked_until)
            VALUES (?, ?, 0, 0)
            ON CONFLICT(phone) DO UPDATE SET
              hash = excluded.hash, wrong_tries = 0, locked_until = 0
            """.trimIndent(),
        ).use { st ->
            st.setString(1, phoneE164)
            st.setString(2, Passwords.hash(password))
            st.executeUpdate()
        }
        return null
    }

    /** Whether this phone has a password at all. Never exposed by a route. */
    fun hasPassword(phoneE164: String): Boolean = read(phoneE164) != null

    /**
     * Check a password. Null when it is right, `WRONG_PASSWORD` otherwise.
     *
     * One refusal covers wrong, locked out, and no such account, on purpose.
     * Telling them apart would answer two questions nobody should be able to
     * ask this endpoint: whether a phone number is registered here, and
     * whether the lockout has kicked in. It is the same rule the one-time
     * code follows, where `WRONG_CODE` also means expired and out of tries.
     */
    fun check(phoneE164: String, password: String): String? {
        val row = read(phoneE164)

        // Hash even when there is no such account, against a throwaway hash
        // of the same shape. Skipping it would make an unknown number answer
        // in a millisecond and a known one in tens of them, and that gap is
        // a way to ask "is this person registered?" without being told.
        val ok = Passwords.matches(password, row?.hash ?: ABSENT)
        if (row == null) return "WRONG_PASSWORD"

        if (row.lockedUntil > now()) return "WRONG_PASSWORD"
        if (!ok) return recordWrongTry(phoneE164, row)

        if (row.wrongTries != 0) write(phoneE164, 0, 0)
        return null
    }

    private fun recordWrongTry(phoneE164: String, row: Row): String {
        val tries = row.wrongTries + 1
        if (tries >= WRONG_TRIES_BEFORE_LOCK) {
            write(phoneE164, 0, now() + LOCK_MS)
        } else {
            write(phoneE164, tries, 0)
        }
        return "WRONG_PASSWORD"
    }

    private fun read(phoneE164: String): Row? =
        db.prepareStatement(
            "SELECT hash, wrong_tries, locked_until FROM accounts WHERE phone = ?",
        ).use { st ->
            st.setString(1, phoneE164)
            st.executeQuery().use { rs ->
                if (rs.next()) Row(rs.getString(1), rs.getInt(2), rs.getLong(3)) else null
            }
        }

    private fun write(phoneE164: String, tries: Int, lockedUntil: Long) {
        db.prepareStatement(
            "UPDATE accounts SET wrong_tries = ?, locked_until = ? WHERE phone = ?",
        ).use { st ->
            st.setInt(1, tries)
            st.setLong(2, lockedUntil)
            st.setString(3, phoneE164)
            st.executeUpdate()
        }
    }

    override fun close() = db.close()

    private companion object {
        const val WRONG_TRIES_BEFORE_LOCK = 5
        const val LOCK_MS = 15L * 60 * 1000

        /** Hashed once, so a request for an unknown phone still does the work. */
        val ABSENT: String = Passwords.hash("no account with this number exists")
    }
}
