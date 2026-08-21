package vn.edu.vgu.smartlocker.server

import java.io.File
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The one property the hashing key exists to give.
 *
 * A pickup code is six digits. Before 2026-08-21 the digest column was a bare
 * SHA-256, so anybody holding `data/locker.db` could build the whole table in
 * about a second and read every live code out of it. These tests say that the
 * table no longer works, and that two servers with different keys cannot read
 * each other's - which is the same statement twice, from both ends.
 *
 * They also pin the shape of the digest. A change there is a change every
 * stored hash in every database disagrees with, and it should be loud.
 */
class PepperTest {

    private fun keyed(secret: String, key: String): String {
        val file = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        file.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(key.toByteArray()))
        Pepper.load(file)
        return Ids.hash(secret)
    }

    @Test
    fun `a stolen database alone no longer reads a code`() {
        // The attack, written out: build the table of all six-digit codes the
        // way it used to work, then look up a digest the server would store.
        val bare = { code: String ->
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(code.toByteArray()),
            )
        }
        val stored = keyed("004271", "the key that is not in the database file")

        // A million entries would take a second; ten thousand is enough to
        // make the point and keeps this test instant. If the digest were still
        // bare, `004271` would be in here.
        val table = (0..9999).associate { bare("%06d".format(it)) to "%06d".format(it) }
        assertEquals(null, table[stored], "the old lookup table still recovers the code")
        assertNotEquals(bare("004271"), stored, "the digest is still a bare SHA-256")
    }

    @Test
    fun `two servers with different keys do not share digests`() {
        assertNotEquals(keyed("004271", "one key"), keyed("004271", "another key"))
    }

    @Test
    fun `the same key gives the same digest, or nobody could log in twice`() {
        assertEquals(keyed("004271", "one key"), keyed("004271", "one key"))
    }

    @Test
    fun `the digest is the same shape as before, so no column changes`() {
        // 32 bytes, url-safe base64, no padding. Every stored hash is this.
        val digest = keyed("a token", "one key")
        assertEquals(43, digest.length)
        assertTrue(digest.none { it == '+' || it == '/' || it == '=' }, "not url-safe: $digest")
    }
}
