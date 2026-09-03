package vn.edu.vgu.smartlocker.server

import java.io.File
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * What a nested [Db.transaction] does. Found while building the booking.
 *
 * `Bookings.create` opens a transaction and calls `Boxes.claimFree`, which
 * opens one of its own. Before this was fixed the inner block's `finally` put
 * the connection back in auto-commit, so the outer `commit()` threw
 * `database in auto-commit mode` - **after the inner block had already
 * committed its half of the work**. A door was claimed, the booking that
 * should have gone with it was not written, and the caller saw an error.
 *
 * The rule: nesting joins the open transaction. The outermost block is the
 * unit, so a failure anywhere in it undoes all of it.
 */
class DbTransactionTest {

    private lateinit var file: File
    private lateinit var db: Db

    @BeforeTest
    fun open() {
        val key = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        key.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 3 }))
        Pepper.load(key)
        file = File.createTempFile("dbtx", ".db").apply { delete() }
        db = Db(file)
    }

    @AfterTest
    fun close() {
        db.close()
        file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
    }

    private fun add(id: String, phone: String) = db.exec(
        "INSERT INTO receivers (id, phone, full_name, created_at) VALUES (?, ?, '', 0)", id, phone,
    )

    private fun count() = db.row("SELECT COUNT(*) AS n FROM receivers") { it.getInt("n") }

    @Test
    fun `a transaction inside a transaction does not throw`() {
        db.transaction {
            add("r1", "+84912340001")
            db.transaction { add("r2", "+84912340002") }
        }
        assertEquals(2, count())
    }

    /**
     * The half that matters. The inner block finished cleanly and its row
     * must still be gone, because the **outer** block failed.
     */
    @Test
    fun `an inner block that succeeded is undone when the outer one fails`() {
        assertFailsWith<IllegalStateException> {
            db.transaction {
                db.transaction { add("r1", "+84912340001") }
                error("the outer block gives up here")
            }
        }
        assertEquals(0, count())
    }

    /** And the connection is usable afterwards, in both directions. */
    @Test
    fun `the connection still works after a nested rollback`() {
        runCatching { db.transaction { db.transaction { add("r1", "+84912340001") }; error("no") } }
        db.transaction { add("r2", "+84912340002") }
        assertEquals(1, count())
    }
}
