package vn.edu.vgu.smartlocker.server.cabinet

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.refuse
import vn.edu.vgu.smartlocker.server.str

/**
 * The cabinets, and the key each one signs in with.
 *
 * A cabinet signs in as a **device, not as a person**. There is no login on a
 * cabinet screen and there never will be - it stands in a lobby and anybody
 * can walk up to it.
 *
 * **Only the hash of a key is stored**, so this database file cannot be turned
 * into a working cabinet. The key itself is placed on the device at setup and
 * is never in this repo - task P0-05, and `src/cabinet/net.js` reads it from
 * the device's own config for the same reason.
 *
 * One key per cabinet, never one shared master. A stolen cabinet costs that
 * cabinet, and [rotate] is how it stops costing anything.
 */
object Cabinets {

    /** Add a cabinet and hand back its key. The only time the key exists here. */
    fun create(db: Db, id: String, name: String, boxes: Int): String {
        val key = Ids.secret()
        db.transaction {
            db.exec(
                "INSERT INTO cabinets (id, name, key_hash, created_at) VALUES (?, ?, ?, ?)",
                id, name, Ids.hash(key), now(),
            )
            (1..boxes).forEach { n ->
                db.exec(
                    "INSERT OR IGNORE INTO boxes (cabinet_id, number, size, state) VALUES (?, ?, ?, 'free')",
                    id, n.toString().padStart(2, '0'), sizeFor(n),
                )
            }
        }
        return key
    }

    /** Give a cabinet a new key and kill the old one. For a stolen cabinet. */
    fun rotate(db: Db, id: String): String? {
        if (find(db, id) == null) return null
        val key = Ids.secret()
        db.exec("UPDATE cabinets SET key_hash = ? WHERE id = ?", Ids.hash(key), id)
        return key
    }

    fun find(db: Db, id: String): String? =
        db.row("SELECT name FROM cabinets WHERE id = ?", id) { it.str("name") }

    fun idForKey(db: Db, key: String): String? =
        db.row("SELECT id FROM cabinets WHERE key_hash = ?", Ids.hash(key)) { it.str("id") }

    fun all(db: Db): List<Pair<String, String>> =
        db.rows("SELECT id, name FROM cabinets ORDER BY id") { it.str("id") to it.str("name") }

    /**
     * A rough spread of sizes so a cabinet is not all one shape.
     *
     * Guessed, not measured - there is no cabinet yet. It is here rather than
     * in the settings file because it only applies the moment a cabinet is
     * created, and changing it later would not move any metal.
     */
    private fun sizeFor(n: Int) = when {
        n % 5 == 0 -> "large"
        n % 3 == 0 -> "small"
        else -> "medium"
    }
}

/**
 * The header a cabinet proves itself with.
 *
 * Named here rather than written twice: the CORS setup in `Main.kt` has to
 * allow exactly this header, and two spellings of it would fail as a browser
 * silently dropping every cabinet call.
 */
const val CABINET_KEY_HEADER = "X-Cabinet-Key"

/**
 * Which cabinet is calling, or refuse.
 *
 * The header is `X-Cabinet-Key`, which is what `src/cabinet/net.js` sends and
 * what the ESP32 firmware sends. A missing key and a wrong key answer the same
 * thing, because telling them apart says whether a guess was close.
 */
suspend fun ApplicationCall.cabinetId(db: Db): String {
    val key = request.header(CABINET_KEY_HEADER)?.trim()?.takeIf(String::isNotEmpty)
        ?: refuse(Refusal.CABINET_UNKNOWN)
    return Cabinets.idForKey(db, key) ?: refuse(Refusal.CABINET_UNKNOWN)
}
