package vn.edu.vgu.smartlocker.server

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("pepper")

/**
 * The key that makes a stolen database file useless.
 *
 * **The problem this solves.** A pickup code is six digits, so there are a
 * million of them. A plain SHA-256 of six digits is a table anybody can build
 * in about a second on a laptop: take the `code_hash` column out of the
 * database file, look every row up, and every live parcel in the cabinet is
 * yours for the next forty-eight hours. Hashing did nothing, because the input
 * space was small enough to enumerate. The same is true of the one-time codes
 * that log a person in.
 *
 * The fix is not a longer hash or a slower one. It is a secret the attacker
 * does not have, mixed into every digest - so the table cannot be built at all
 * without first stealing a second thing from somewhere else. That is this key,
 * and [Ids.hash] is an HMAC under it rather than a bare digest.
 *
 * **It lives outside `data/`, and that is the whole point.** The database file
 * and its backups are the thing that gets copied - onto a laptop to look at a
 * bug, into a folder somebody shares, onto a disk that is thrown away. If the
 * key sat beside `locker.db` it would travel in every one of those copies and
 * buy nothing. The default is `config/pepper.key`, git-ignored, and a real
 * deployment sets `LOCKER_PEPPER` in the environment and has no file at all.
 *
 * **A new key voids every hash ever written.** Tokens, one-time codes, pickup
 * codes and cabinet keys were all recorded under the old one and cannot be
 * re-derived - that is what a one-way function means. Losing this file is
 * therefore not a small thing: every phone is logged out, every code in the
 * air is dead, and every cabinet has to be rotated by hand. Back it up
 * separately from the database, or it is a single point of failure wearing a
 * security hat.
 */
object Pepper {

    private val random = SecureRandom()

    @Volatile private var key: ByteArray? = null

    @Volatile private var version = 0

    /**
     * Where the key comes from, resolved once at startup.
     *
     * The environment wins, because a deployment that can set a variable
     * should never have a copy on disk. Otherwise the file is read, and if it
     * is not there yet one is made - a server on a clean clone has to start,
     * and refusing to would only teach whoever met it to set the variable to
     * something short and memorable.
     */
    fun load(file: File) {
        val fromEnv = System.getenv("LOCKER_PEPPER")?.trim()?.takeIf(String::isNotEmpty)
        key = when {
            fromEnv != null -> {
                log.info("pepper    from LOCKER_PEPPER")
                fromEnv.toByteArray(Charsets.UTF_8)
            }
            file.exists() -> {
                log.info("pepper    {}", file.absolutePath)
                Base64.getUrlDecoder().decode(file.readText().trim())
            }
            else -> make(file)
        }
        version++
    }

    /**
     * Which key this is, counting from the first one loaded.
     *
     * Only exists so that anything holding a keyed object can notice the key
     * underneath it changed. In a running server that never happens; in a test
     * that loads two keys, not noticing would make the test lie.
     */
    fun version(): Int = version

    /** The key, or a refusal to run without one. */
    fun bytes(): ByteArray = key ?: error(
        "Pepper.load() was not called before something was hashed. " +
            "It must run first, or half the digests in a file are under a key nobody kept.",
    )

    /**
     * A fresh key, written down before it is used.
     *
     * Loud on purpose. The line it prints is the only warning anybody gets
     * that an existing database has just become unreadable - which happens
     * exactly once per machine, on the first start after this shipped, and is
     * otherwise seen as "everybody was logged out overnight".
     */
    private fun make(file: File): ByteArray {
        val fresh = ByteArray(32).also(random::nextBytes)
        file.parentFile?.mkdirs()
        file.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(fresh))
        // Best effort - Windows ignores the group and world bits, and there is
        // no portable way to say "only me" from the JDK. On a Pi it holds.
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)
        log.warn("")
        log.warn("A NEW hashing key was written to {}", file.absolutePath)
        log.warn("  Every token, one-time code and pickup code recorded before now is dead.")
        log.warn("  Every cabinet must be rotated:  cabinet rotate <id>")
        log.warn("  Back this file up somewhere that is NOT beside data/locker.db.")
        log.warn("")
        return fresh
    }
}
