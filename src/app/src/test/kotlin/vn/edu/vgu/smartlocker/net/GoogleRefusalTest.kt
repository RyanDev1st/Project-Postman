package vn.edu.vgu.smartlocker.net

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app understands every way the server can refuse a Google sign-in.
 * Task P2-13, [ADR 0026].
 *
 * Two enums with the same names in two modules is a contract held together by
 * nothing but people remembering. When the server learned to refuse an
 * account from the wrong domain, an app that did not know the word
 * `GOOGLE_DOMAIN` would have shown "Something went wrong" to a student who
 * had simply tapped the button with their personal Gmail - and there is no
 * second clue anywhere on the screen for them to work it out from.
 *
 * So this reads the **server's** file and checks the app's list against it.
 * It costs one file read and it fails the build the moment the two drift,
 * which is months before anybody would notice by hand.
 */
class GoogleRefusalTest {

    /** The server's own list, wherever the test happens to be run from. */
    private val serverEnum = listOf(
        File("../server/src/main/kotlin/vn/edu/vgu/smartlocker/server/Refusals.kt"),
        File("src/server/src/main/kotlin/vn/edu/vgu/smartlocker/server/Refusals.kt"),
    ).firstOrNull { it.isFile }

    private fun serverCodes(prefix: String): List<String> {
        assertNotNull("could not find the server's Refusals.kt", serverEnum)
        return Regex("""^\s{4}($prefix\w*)\(HttpStatusCode""", RegexOption.MULTILINE)
            .findAll(serverEnum!!.readText())
            .map { it.groupValues[1] }
            .toList()
    }

    @Test
    fun `the app knows every Google refusal the server can send`() {
        val sent = serverCodes("GOOGLE_")
        assertTrue("the server sends no GOOGLE_ refusals at all", sent.isNotEmpty())
        val unknown = sent.filter { Refusal.of(it) == Refusal.UNKNOWN }
        assertEquals("the server can refuse with codes this app does not know", emptyList<String>(), unknown)
    }

    @Test
    fun `every Google refusal has a sentence to show`() {
        for (code in serverCodes("GOOGLE_")) {
            assertNotNull(
                "$code parses but has no sentence, so the screen would draw nothing",
                Refusal.of(code).message,
            )
        }
    }

    /**
     * The wrong-domain refusal has to be its own sentence.
     *
     * It is the one refusal in the app that says why, because the person
     * reading it has done nothing wrong and cannot guess the fix. If somebody
     * ever points it at the same string as the others to "keep failures
     * uniform", this says no.
     */
    @Test
    fun `the wrong domain says something the other Google refusals do not`() {
        assertTrue(
            "the domain refusal must not share a sentence with the flat ones",
            Refusal.GOOGLE_DOMAIN.message != Refusal.GOOGLE_INVALID.message &&
                Refusal.GOOGLE_DOMAIN.message != Refusal.UNKNOWN.message,
        )
    }

    /** A newer server must never crash an older app. */
    @Test
    fun `a code this build has never heard of is not fatal`() {
        assertEquals(Refusal.UNKNOWN, Refusal.of("GOOGLE_SOMETHING_FROM_NEXT_YEAR"))
        assertEquals(Refusal.UNKNOWN, Refusal.of(""))
    }

    /** The server sends upper case; nothing should depend on that. */
    @Test
    fun `the code is read whatever case it arrives in`() {
        assertEquals(Refusal.GOOGLE_DOMAIN, Refusal.of("google_domain"))
        assertEquals(Refusal.GOOGLE_DOMAIN, Refusal.of("  GOOGLE_DOMAIN  "))
    }
}
