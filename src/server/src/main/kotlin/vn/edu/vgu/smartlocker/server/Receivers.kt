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
    fun findOrCreate(db: Db, phone: String, fullName: String = ""): String = db.transaction {
        val name = tidyName(fullName)
        val existing = find(db, phone)
        if (existing != null) {
            // Filled in only while it is empty. A person can name an account
            // they never named, and nobody can rename one that is already
            // named - including the person themselves, who would have to ask.
            // Silent renaming is how the shipper ends up confirming a name
            // that no longer belongs to whoever is collecting.
            if (name.isNotEmpty() && this.name(db, existing).isEmpty()) {
                db.exec("UPDATE receivers SET full_name = ? WHERE id = ?", name, existing)
            }
            return@transaction existing
        }
        Ids.id().also { id ->
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, ?, ?)",
                id, phone, name, now(),
            )
        }
    }

    /**
     * What is safe to keep out of a name somebody typed on a phone.
     *
     * Capped at 60 characters, and control characters removed. The cabinet
     * screen draws this with `textContent`, so there is no markup to escape -
     * this is about a name that would break the layout of a public screen, and
     * about a database row that grows without limit.
     */
    private fun tidyName(raw: String): String =
        raw.filter { it.code >= 0x20 }.trim().replace(Regex("\\s+"), " ").take(60)

    fun find(db: Db, phone: String): String? =
        db.row("SELECT id FROM receivers WHERE phone = ?", phone) { it.str("id") }

    fun name(db: Db, receiverId: String): String =
        db.row("SELECT full_name FROM receivers WHERE id = ?", receiverId) { it.str("full_name") }
            .orEmpty()
}
