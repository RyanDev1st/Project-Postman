package vn.edu.vgu.smartlocker.server

/**
 * The people parcels belong to.
 *
 * **A receiver is created by signing in with Google on a university domain,
 * and by nothing else.** There is no register-with-a-password route: a
 * password is set later, on an account that already exists.
 *
 * Until 2026-09-02 the only door was a one-time SMS code, because the shipper
 * finds a receiver by typing a **phone number** and an account without one
 * could never be sent a parcel (ADR 0012). ADR 0026 answers that differently -
 * the number arrives at the first booking, typed by somebody already signed
 * in, against a delivery they are expecting themselves. So `phone` is NULL
 * until then, and an account with no number is a normal state rather than a
 * broken one.
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

    // --- The number, claimed later - endpoints 25 and 29, task P2-13 -------

    /**
     * Claim a phone number for this account, or say who already has it.
     *
     * Returns true when the number is now this account's. **False means
     * somebody else holds it**, and the caller answers `PHONE_IN_USE` naming
     * nobody - first claim wins, and a second person typing those digits has
     * almost certainly mistyped their own.
     *
     * Setting the same number again is true and changes nothing, so a phone
     * that retries a call it never saw the answer to is not punished for it.
     *
     * The check and the write are one transaction. Read-then-write across two
     * would let two accounts both see a free number and both take it; what
     * actually stops that is the `UNIQUE` on the column, and this only turns
     * the constraint into a refusal a person can read.
     */
    fun claimPhone(db: Db, receiverId: String, phone: String): Boolean = db.transaction {
        val holder = find(db, phone)
        if (holder != null) return@transaction holder == receiverId
        db.exec("UPDATE receivers SET phone = ? WHERE id = ?", phone, receiverId)
        true
    }

    /** Whether this account has claimed a number yet. Endpoint 25 asks first. */
    fun hasPhone(db: Db, receiverId: String): Boolean = phone(db, receiverId).isNotEmpty()

    // --- The Google link - endpoint 19, tasks P2-08 and P2-11 --------------

    /**
     * The receiver behind this Google account, making one if it is new.
     *
     * This is the only route that creates an account, and it is reached only
     * after `GoogleTokens` has checked the signature **and** the hosted
     * domain - so the row it writes belongs to somebody the university
     * vouches for. ADR 0026.
     *
     * The name comes from the Google profile the app read, and is filled in
     * only while it is empty, exactly as [findOrCreate] does and for the same
     * reason: silent renaming is how a shipper ends up confirming a name that
     * no longer belongs to whoever collects.
     *
     * `phone` stays NULL. It is claimed at the first booking.
     */
    fun findOrCreateByGoogle(db: Db, sub: String, fullName: String = ""): String = db.transaction {
        val name = tidyName(fullName)
        val existing = findByGoogle(db, sub)
        if (existing != null) {
            if (name.isNotEmpty() && this.name(db, existing).isEmpty()) {
                db.exec("UPDATE receivers SET full_name = ? WHERE id = ?", name, existing)
            }
            return@transaction existing
        }
        Ids.id().also { id ->
            db.exec(
                "INSERT INTO receivers (id, phone, full_name, created_at, google_sub) " +
                    "VALUES (?, NULL, ?, ?, ?)",
                id, name, now(), sub,
            )
        }
    }

    /**
     * The receiver a Google account is tied to, or null while it is unlinked.
     *
     * Keyed by Google's `sub` and never by the email address: a person can
     * change their Gmail address, and `sub` cannot be changed for as long as
     * the account lives.
     */
    fun findByGoogle(db: Db, sub: String): String? =
        db.row("SELECT id FROM receivers WHERE google_sub = ?", sub) { it.str("id") }

    /**
     * Tie a Google account to a receiver.
     *
     * Doing it again re-points it, and that is safe rather than sloppy: it
     * takes both the Google account **and** a one-time code on the new phone,
     * and anybody holding both is the same person. It is how somebody who
     * changes phone number keeps their Google sign-in.
     *
     * The unique index does the rest. Linking a Google account that is
     * already tied to a different receiver fails here rather than quietly
     * leaving two rows a sign-in could pick between - and the row it picked
     * would be somebody else's parcels.
     */
    fun linkGoogle(db: Db, receiverId: String, sub: String) = db.transaction {
        // Cleared from whoever held it, so re-pointing does not trip the
        // unique index against a row that is about to stop using it.
        db.exec("UPDATE receivers SET google_sub = NULL WHERE google_sub = ?", sub)
        db.exec("UPDATE receivers SET google_sub = ? WHERE id = ?", sub, receiverId)
    }

    fun name(db: Db, receiverId: String): String =
        db.row("SELECT full_name FROM receivers WHERE id = ?", receiverId) { it.str("full_name") }
            .orEmpty()

    /** The number in the form it was stored, for endpoint 24 and nothing else. */
    fun phone(db: Db, receiverId: String): String =
        db.row("SELECT phone FROM receivers WHERE id = ?", receiverId) { it.str("phone") }
            .orEmpty()
}
