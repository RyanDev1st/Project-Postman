package vn.edu.vgu.smartlocker.server

import java.io.File
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What endpoint 24 hands back. BUG-021.
 *
 * The bug was that the app had no way to ask, so `SettingsScreen` and the
 * Home greeting drew the parameter defaults they were written with and a
 * receiver registered as Tran Thi Mai was greeted as Minh. The fix is a
 * route, and a route is only worth having if it reads back the receiver the
 * token belongs to and no other.
 *
 * These are the two reads the route makes, on a real database file. The route
 * itself is one line each side of them.
 */
class AccountReadbackTest {

    private lateinit var file: File
    private lateinit var db: Db

    private val mai = "+84912340333"
    private val minh = "+84912340444"

    @BeforeTest
    fun open() {
        val key = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        key.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }))
        Pepper.load(key)
        file = File.createTempFile("accountreadback", ".db").apply { delete() }
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
    fun `an account reads back its own name and number`() {
        val me = Receivers.findOrCreate(db, mai, "Tran Thi Mai")
        assertEquals("Tran Thi Mai", Receivers.name(db, me))
        assertEquals(mai, Receivers.phone(db, me))
    }

    @Test
    fun `two accounts never read back each other`() {
        val hers = Receivers.findOrCreate(db, mai, "Tran Thi Mai")
        val his = Receivers.findOrCreate(db, minh, "Minh Nguyen")

        assertEquals("Tran Thi Mai", Receivers.name(db, hers))
        assertEquals("Minh Nguyen", Receivers.name(db, his))
        assertEquals(mai, Receivers.phone(db, hers))
        assertEquals(minh, Receivers.phone(db, his))
    }

    /**
     * An account with no name is answered with no name.
     *
     * The app draws a blank rather than a guess, which is the whole point:
     * every way in except the one-time code arrives without a name, and the
     * greeting has to say nothing rather than say somebody else.
     */
    @Test
    fun `an account with no name reads back empty, never a placeholder`() {
        val me = Receivers.findOrCreate(db, minh)
        assertEquals("", Receivers.name(db, me))
        assertEquals(minh, Receivers.phone(db, me))
    }

    /** A receiver id nobody holds is empty, not an exception and not somebody. */
    @Test
    fun `an unknown receiver reads back empty`() {
        assertEquals("", Receivers.name(db, "r_nobody"))
        assertEquals("", Receivers.phone(db, "r_nobody"))
    }
}
