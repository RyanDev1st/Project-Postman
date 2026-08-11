package otp.password

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasswordsTest {

    @Test
    fun aPasswordMatchesItsOwnHash() {
        val stored = Passwords.hash("correct horse battery")
        assertTrue(Passwords.matches("correct horse battery", stored))
    }

    @Test
    fun anythingElseDoesNot() {
        val stored = Passwords.hash("correct horse battery")
        listOf("", "correct horse batter", "correct horse batteryy", "CORRECT HORSE BATTERY")
            .forEach { assertFalse(Passwords.matches(it, stored), "accepted <$it>") }
    }

    @Test
    fun theSamePasswordHashesDifferentlyEveryTime() {
        // A fresh salt per hash. Without it, two people who picked the same
        // password would show up as the same row, and one cracked hash would
        // open both.
        val a = Passwords.hash("same password")
        val b = Passwords.hash("same password")
        assertTrue(a != b, "two hashes of one password came out identical")
        assertTrue(Passwords.matches("same password", a))
        assertTrue(Passwords.matches("same password", b))
    }

    @Test
    fun theStoredFormCarriesItsOwnParameters() {
        // So raising the cost later does not strand passwords hashed before.
        val parts = Passwords.hash("a password").split("$")
        assertEquals(6, parts.size)
        assertEquals("argon2id", parts[0])
        assertEquals(19 * 1024, parts[1].toInt())
        assertEquals(2, parts[2].toInt())
    }

    @Test
    fun aStoredHashThatHasBeenEditedIsRefused() {
        val stored = Passwords.hash("a password")
        listOf(
            "",
            "not a hash",
            stored.replace("argon2id", "argon2i"),
            stored.split("$").dropLast(1).joinToString("$"),
            stored.dropLast(4),
        ).forEach { assertFalse(Passwords.matches("a password", it), "accepted <$it>") }
    }

    @Test
    fun tooShortAndTooLongAreRefused() {
        assertEquals("PASSWORD_TOO_SHORT", Passwords.refuse("short"))
        assertEquals("PASSWORD_TOO_SHORT", Passwords.refuse(""))
        assertEquals("PASSWORD_TOO_LONG", Passwords.refuse("x".repeat(201)))
        assertNull(Passwords.refuse("12345678"))
        assertNull(Passwords.refuse("x".repeat(200)))
    }
}
