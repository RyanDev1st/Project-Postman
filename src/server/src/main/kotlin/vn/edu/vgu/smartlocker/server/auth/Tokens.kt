package vn.edu.vgu.smartlocker.server.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.refuse
import vn.edu.vgu.smartlocker.server.str

/**
 * The receiver's pass, and who is behind it.
 *
 * A token proves *which person is asking*, and it is the only thing that does.
 * The QR on the cabinet proves *which cabinet*, and the two together are what
 * open a door - see "The scanned code - what protects it" in the contract.
 *
 * **Only the hash is stored.** A database file that leaks is then a list of
 * digests nobody can log in with. The token itself exists in one place: the
 * phone's secure store.
 */
class Tokens(private val db: Db, private val lifetimeDays: Long) {

    /** A token and the moment it stops working. */
    data class Issued(val token: String, val expiresAt: Long)

    /** Hand a fresh pass to a receiver who has just proved themselves. */
    fun mint(receiverId: String): Issued {
        val token = Ids.secret()
        val expiresAt = now() + lifetimeDays * 24 * 60 * 60 * 1000
        db.exec(
            "INSERT INTO tokens (token_hash, receiver_id, expires_at) VALUES (?, ?, ?)",
            Ids.hash(token), receiverId, expiresAt,
        )
        return Issued(token, expiresAt)
    }

    /**
     * Who this token belongs to, or null.
     *
     * Null covers three cases on purpose - never issued, already logged out,
     * and run out of time. The caller answers `TOKEN_EXPIRED` for all three,
     * because telling them apart says whether a guessed token ever existed.
     */
    fun receiverFor(token: String): String? = db.row(
        "SELECT receiver_id FROM tokens WHERE token_hash = ? AND expires_at > ?",
        Ids.hash(token), now(),
    ) { it.str("receiver_id") }

    /** Swap a tiring pass for a fresh one. The old one dies here. */
    fun refresh(token: String): Issued? {
        val receiverId = receiverFor(token) ?: return null
        return db.transaction {
            revoke(token)
            mint(receiverId)
        }
    }

    fun revoke(token: String) =
        db.exec("DELETE FROM tokens WHERE token_hash = ?", Ids.hash(token))

    /** Drop what has run out. Cheap, and keeps the table from growing forever. */
    fun sweep() = db.exec("DELETE FROM tokens WHERE expires_at <= ?", now())
}

/**
 * The token on this request, or nothing.
 *
 * Reads `Authorization: Bearer …`, which is what the app's `Http.kt` sends.
 */
fun ApplicationCall.bearer(): String? =
    request.header("Authorization")
        ?.trim()
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substring(7)
        ?.trim()
        ?.takeIf(String::isNotEmpty)

/**
 * Who is asking, or refuse the call.
 *
 * Every route that touches a person's parcels starts with this line. It
 * returns a receiver id and never a token, so nothing downstream can log one
 * by accident.
 */
suspend fun ApplicationCall.receiverId(tokens: Tokens): String {
    val token = bearer() ?: refuse(Refusal.TOKEN_EXPIRED)
    return tokens.receiverFor(token) ?: refuse(Refusal.TOKEN_EXPIRED)
}
