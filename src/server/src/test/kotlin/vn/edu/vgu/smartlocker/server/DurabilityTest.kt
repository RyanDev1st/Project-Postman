package vn.edu.vgu.smartlocker.server

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The lockout counter has to survive a power cut.
 *
 * That is a rule about this product, not about SQLite: a box lockout and a
 * password lockout are both counters that an attacker gets to reset for free
 * if pulling the plug loses the last write. Five guesses that only cost a
 * power cycle is not a lockout.
 *
 * The counters are rows, so the guarantee is entirely `PRAGMA synchronous`.
 * `2` is FULL - every commit is on the disk before it is called committed.
 *
 * Nothing in `Db` sets it: it is the sqlite-jdbc driver's default. That is
 * exactly why this test exists. A driver upgrade that changed the default to
 * NORMAL - which is what SQLite's own documentation recommends alongside WAL,
 * and what several drivers do - would take the guarantee away with no
 * compile error and no failing test anywhere else. This is the failing test.
 *
 * The throughput this costs was measured, not guessed: the load runs in
 * `docs/findings/2026-08-21-hardening-and-load.md` were all made with FULL.
 */
class DurabilityTest {

    @Test
    fun `every commit is on the disk before it is called committed`() {
        onATempDb { db ->
            assertEquals(2, db.row("PRAGMA synchronous") { it.getInt(1) })
        }
    }

    @Test
    fun `and the write-ahead log is on, so readers never block the writer`() {
        onATempDb { db ->
            assertEquals("wal", db.row("PRAGMA journal_mode") { it.getString(1) })
        }
    }

    private fun onATempDb(work: (Db) -> Unit) {
        val file = File.createTempFile("durability", ".db").also { it.delete() }
        try {
            Db(file).use(work)
        } finally {
            file.delete()
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
        }
    }
}
