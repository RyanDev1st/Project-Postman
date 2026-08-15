package vn.edu.vgu.smartlocker.server

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * The database, and the only way to reach it.
 *
 * SQLite, in one file. Not MySQL - ADR 0020. The reason is not that it is
 * smaller: it is that two rules in this project need state that survives the
 * process dying, and a file gives that for nothing.
 *
 * - **The lockout counter must survive a power cut.** A counter in memory
 *   resets when the process dies, so anybody who can crash the server gets
 *   their guesses back.
 * - **A QR session must survive a restart.** The server has to remember what
 *   it handed out. If a restart forgets, every code in the air becomes
 *   forgeable, because nothing is left to compare against.
 *
 * One connection behind one lock. SQLite serialises writes anyway, and a pool
 * would buy contention for concurrency this project does not have - a cabinet
 * or two, and a few hundred students.
 */
class Db(file: File) : AutoCloseable {

    private val lock = Any()
    private val conn: Connection

    init {
        file.parentFile?.mkdirs()
        Class.forName("org.sqlite.JDBC")
        conn = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        conn.autoCommit = true
        exec("PRAGMA journal_mode=WAL")
        // Without this, SQLite parses a REFERENCES clause and then ignores it,
        // which is how a delivery ends up pointing at a parcel that is gone.
        exec("PRAGMA foreign_keys=ON")
        exec("PRAGMA busy_timeout=5000")
        Schema.create(this)
    }

    /**
     * Run a statement whose answer nobody wants.
     *
     * `execute` rather than `executeUpdate`, because some of these are
     * pragmas: `PRAGMA journal_mode=WAL` hands back the mode it settled on,
     * and `executeUpdate` throws "Query returns results" rather than ignoring
     * a row nobody asked for.
     */
    fun exec(sql: String, vararg args: Any?): Unit = synchronized(lock) {
        conn.prepareStatement(sql).use { it.bind(args); it.execute() }
    }

    /** Run a statement and say how many rows it changed. */
    fun update(sql: String, vararg args: Any?): Int = synchronized(lock) {
        conn.prepareStatement(sql).use { it.bind(args); it.executeUpdate() }
    }

    /** Read every matching row, mapped one at a time. */
    fun <T> rows(sql: String, vararg args: Any?, map: (ResultSet) -> T): List<T> =
        synchronized(lock) {
            conn.prepareStatement(sql).use { st ->
                st.bind(args)
                st.executeQuery().use { rs ->
                    val out = ArrayList<T>()
                    while (rs.next()) out.add(map(rs))
                    out
                }
            }
        }

    /** Read the first matching row, or null. */
    fun <T> row(sql: String, vararg args: Any?, map: (ResultSet) -> T): T? =
        rows(sql, *args, map = map).firstOrNull()

    /**
     * Run several statements as one, or none of them.
     *
     * Used wherever a half-finished write would be worse than no write - the
     * collect path writes an event, marks a parcel and queues a door command,
     * and a crash between the second and the third would open a door the log
     * never mentions.
     */
    fun <T> transaction(body: () -> T): T = synchronized(lock) {
        conn.autoCommit = false
        try {
            val out = body()
            conn.commit()
            out
        } catch (e: Throwable) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    override fun close() = synchronized(lock) { conn.close() }
}

/** Bind whatever the caller passed, in order, with no type ceremony. */
private fun PreparedStatement.bind(args: Array<out Any?>) {
    args.forEachIndexed { i, a ->
        when (a) {
            null -> setNull(i + 1, java.sql.Types.NULL)
            is Int -> setInt(i + 1, a)
            is Long -> setLong(i + 1, a)
            is Boolean -> setInt(i + 1, if (a) 1 else 0)
            is ByteArray -> setBytes(i + 1, a)
            else -> setString(i + 1, a.toString())
        }
    }
}

/** Read a column that may be absent from this row's SELECT. Null if it is. */
fun ResultSet.str(name: String): String = getString(name) ?: ""

fun ResultSet.num(name: String): Long = getLong(name)

fun ResultSet.flag(name: String): Boolean = getInt(name) != 0
