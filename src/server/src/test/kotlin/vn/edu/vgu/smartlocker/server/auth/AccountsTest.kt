package vn.edu.vgu.smartlocker.server.auth

import java.io.File
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Pepper
import vn.edu.vgu.smartlocker.server.Receivers
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.str

/**
 * The rules a password has to keep, on a real database file.
 *
 * Not a mock. The one thing [Accounts] exists to guarantee - **five wrong
 * tries stop the right password working, and that survives a power cut** -
 * is a claim about what is on disk, and a test against a fake store would
 * prove the opposite of what matters (ADR 0012).
 *
 * The lockout clock is moved by writing the column rather than by waiting
 * fifteen minutes. That is the same durability being tested from the other
 * side: if the counter were in memory, there would be no column to write.
 */
class AccountsTest {

    private lateinit var file: File
    private lateinit var db: Db
    private lateinit var accounts: Accounts
    private lateinit var me: String

    private val phone = "+84912340999"

    @BeforeTest
    fun open() {
        val key = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        key.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }))
        Pepper.load(key)

        file = File.createTempFile("accounts", ".db").apply { delete() }
        db = Db(file)
        accounts = Accounts(db, triesBeforeLock = 5, lockMinutes = 15)
        me = Receivers.findOrCreate(db, phone, "Password Test")
    }

    @AfterTest
    fun close() {
        db.close()
        file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
    }

    private fun stored(): String =
        db.row("SELECT password_hash FROM receivers WHERE id = ?", me) { it.str("password_hash") }.orEmpty()

    @Test
    fun `a new account has no password, and cannot be signed into`() {
        assertFalse(accounts.hasPassword(me))
        assertNull(accounts.check(phone, ""), "an empty password opened an account with none set")
        assertNull(accounts.check(phone, "anything at all"))
    }

    @Test
    fun `a password that is set can be used, and is not stored as itself`() {
        assertNull(accounts.setPassword(me, "one hard password"))
        assertTrue(accounts.hasPassword(me))
        assertEquals(me, accounts.check(phone, "one hard password"))
        assertFalse(stored().contains("one hard password"), "the password is in the database as itself")
        assertTrue(stored().startsWith("argon2id$"), "not an Argon2id hash: ${stored().take(20)}")
    }

    @Test
    fun `too short is refused, and nothing is written`() {
        assertEquals("PASSWORD_TOO_SHORT", accounts.setPassword(me, "short"))
        assertEquals("PASSWORD_TOO_LONG", accounts.setPassword(me, "x".repeat(201)))
        assertFalse(accounts.hasPassword(me), "a refused password was stored anyway")
    }

    @Test
    fun `five wrong tries stop the right password working`() {
        accounts.setPassword(me, "one hard password")
        repeat(5) { assertNull(accounts.check(phone, "not the password")) }
        assertNull(
            accounts.check(phone, "one hard password"),
            "the right password still worked after five wrong ones - there is no lockout",
        )
    }

    @Test
    fun `the lockout is a row on disk, and it lets go when it expires`() {
        accounts.setPassword(me, "one hard password")
        repeat(5) { accounts.check(phone, "not the password") }

        val until = db.row("SELECT password_locked_until FROM receivers WHERE id = ?", me) {
            it.getLong("password_locked_until")
        }
        assertNotNull(until)
        assertTrue(until > now(), "the lockout is not written down, so a restart would clear it")

        // Fifteen minutes later, without waiting fifteen minutes.
        db.exec("UPDATE receivers SET password_locked_until = ? WHERE id = ?", now() - 1, me)
        assertEquals(me, accounts.check(phone, "one hard password"), "the lockout never let go")
    }

    @Test
    fun `four wrong then a right one clears the count`() {
        accounts.setPassword(me, "one hard password")
        repeat(4) { accounts.check(phone, "not the password") }
        assertEquals(me, accounts.check(phone, "one hard password"))
        // The count is back to zero, so four more wrong ones do not lock it.
        repeat(4) { accounts.check(phone, "not the password") }
        assertEquals(me, accounts.check(phone, "one hard password"))
    }

    @Test
    fun `setting a password clears a lockout`() {
        accounts.setPassword(me, "one hard password")
        repeat(5) { accounts.check(phone, "not the password") }
        assertNull(accounts.check(phone, "one hard password"))

        // They proved themselves another way - a one-time code - and chose a
        // new password. Leaving them locked out of the password they just set
        // would be absurd.
        accounts.setPassword(me, "a different hard password")
        assertEquals(me, accounts.check(phone, "a different hard password"))
    }

    @Test
    fun `a number nobody has registered answers the same as a wrong password`() {
        accounts.setPassword(me, "one hard password")
        assertNull(accounts.check("+84900000001", "one hard password"))
        assertNull(accounts.check(phone, "not the password"))
    }
}
