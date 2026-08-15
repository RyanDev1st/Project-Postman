package vn.edu.vgu.smartlocker.server

/**
 * The people parcels belong to.
 *
 * A receiver is created by proving a phone number with a one-time code, and by
 * nothing else. There is no register-with-a-password route and no
 * register-with-Google route, for the same reason in both cases: the shipper
 * finds the receiver by typing a **phone number** on the cabinet screen, so an
 * account without one could never be sent a parcel. See ADR 0012.
 */
object Receivers {

    /**
     * The receiver behind this number, making one if this is the first time.
     *
     * Registering and signing in are the same act here. A student who wipes
     * their phone types the same number, gets the same code, and finds their
     * parcels where they left them - there is nothing else to remember and so
     * nothing else to lose.
     */
    fun findOrCreate(db: Db, phone: String): String = db.transaction {
        find(db, phone) ?: Ids.id().also { id ->
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                id, phone, "", now(),
            )
        }
    }

    fun find(db: Db, phone: String): String? =
        db.row("SELECT id FROM receivers WHERE phone = ?", phone) { it.str("id") }

    fun name(db: Db, receiverId: String): String =
        db.row("SELECT full_name FROM receivers WHERE id = ?", receiverId) { it.str("full_name") }
            .orEmpty()
}
