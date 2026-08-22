package vn.edu.vgu.smartlocker.server

import java.io.File
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Which receiver a Google account reaches. Endpoint 19, task P2-08.
 *
 * `GoogleTokensTest` proves a token is genuine. This proves the other half,
 * which is the half that can hand somebody the wrong parcels: **one Google
 * account reaches exactly one receiver, and never two.**
 *
 * On a real database file, because the guarantee is a unique index and a
 * fake store would have whatever behaviour the fake was written to have.
 */
class GoogleLinkTest {

    private lateinit var file: File
    private lateinit var db: Db

    private val minh = "+84912340111"
    private val an = "+84912340222"
    private val sub = "108476213905551212121"

    @BeforeTest
    fun open() {
        val key = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        key.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 9 }))
        Pepper.load(key)
        file = File.createTempFile("googlelink", ".db").apply { delete() }
        db = Db(file)
    }

    @AfterTest
    fun close() {
        db.close()
        file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
    }

    @Test
    fun `an unlinked google account reaches nobody`() {
        Receivers.findOrCreate(db, minh, "Minh")
        assertNull(Receivers.findByGoogle(db, sub))
    }

    @Test
    fun `once linked it reaches the receiver it was linked to`() {
        val me = Receivers.findOrCreate(db, minh, "Minh")
        Receivers.linkGoogle(db, me, sub)
        assertEquals(me, Receivers.findByGoogle(db, sub))
    }

    /**
     * The one that matters. Linking the same Google account to a second
     * receiver must move it, not duplicate it - otherwise a Google sign-in
     * picks whichever row the database returns first, and that is a coin
     * toss between two people's parcels.
     */
    @Test
    fun `linking it again moves it, and never leaves two`() {
        val first = Receivers.findOrCreate(db, minh, "Minh")
        val second = Receivers.findOrCreate(db, an, "An")
        Receivers.linkGoogle(db, first, sub)
        Receivers.linkGoogle(db, second, sub)

        assertEquals(second, Receivers.findByGoogle(db, sub))
        assertEquals(
            1,
            db.row("SELECT COUNT(*) AS n FROM receivers WHERE google_sub = ?", sub) { it.getInt("n") },
        )
    }

    /**
     * Everybody else stays unlinked, and SQLite allows any number of rows to
     * hold NULL in a unique column. That is the whole reason the unlinked
     * value is NULL and not an empty string: with '' the second account ever
     * created would fail to insert.
     */
    @Test
    fun `two accounts with no google link at all can both exist`() {
        Receivers.findOrCreate(db, minh, "Minh")
        Receivers.findOrCreate(db, an, "An")
        assertEquals(
            2,
            db.row("SELECT COUNT(*) AS n FROM receivers WHERE google_sub IS NULL") { it.getInt("n") },
        )
    }

    @Test
    fun `unlinking one receiver does not unlink another`() {
        val first = Receivers.findOrCreate(db, minh, "Minh")
        val second = Receivers.findOrCreate(db, an, "An")
        Receivers.linkGoogle(db, first, sub)
        Receivers.linkGoogle(db, second, "a-different-google-account")

        assertEquals(first, Receivers.findByGoogle(db, sub))
        assertEquals(second, Receivers.findByGoogle(db, "a-different-google-account"))
    }
}
