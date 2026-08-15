package vn.edu.vgu.smartlocker.pickup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The rules about the string in the QR, checked without a camera.
 *
 * [readSessionCode] takes `now` as an argument for exactly this reason: every
 * case here runs in a millisecond, on any machine, at any hour, instead of
 * somebody standing at a cabinet with a stopwatch waiting for a code to
 * expire.
 *
 * The other end of this contract is the **server** - `Sessions.format` writes
 * what this file reads. `src/cabinet/qr.js` only carries the string between
 * them, and the last test here is what holds it to that.
 *
 * The shape itself is compared against a real issued code in
 * `scripts/checkserver.py`, which is the only place both ends are running at
 * once.
 */
class SessionCodeTest {

    /** A moment. Any moment; the code carries its own. */
    private val now = 1_786_686_322L

    @Test
    fun `a code the cabinet drew a second ago is ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now - 1}|R4nd0m", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now - 1), read)
    }

    @Test
    fun `spaces around it are the camera's, not the cabinet's`() {
        val read = readSessionCode("  VGU1|vgu-gate-01|$now|R4nd0m\n", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now), read)
    }

    /**
     * The cabinet redraws every 30 seconds and each code lives 60, so a code
     * being a little old is the normal case, not a fault.
     */
    @Test
    fun `a code from forty seconds ago is still ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now - 40}|R4nd0m", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now - 40), read)
    }

    /**
     * 60 seconds of life plus 15 of slack for two clocks that disagree. At 75
     * it still passes; the slack is there to be used, not admired.
     */
    @Test
    fun `the last second of the window is still ours`() {
        val edge = CODE_GOOD_FOR_SECONDS + 15L
        val read = readSessionCode("VGU1|vgu-gate-01|${now - edge}|R4nd0m", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now - edge), read)
    }

    @Test
    fun `a second past the window is stale`() {
        val past = CODE_GOOD_FOR_SECONDS + 16L
        val read = readSessionCode("VGU1|vgu-gate-01|${now - past}|R4nd0m", now)
        assertEquals(Scanned.Stale("vgu-gate-01", past), read)
    }

    /**
     * A photograph of the screen, scanned later. Stale, and the screen says
     * "scan again" — which works, because the cabinet has drawn a new one.
     */
    @Test
    fun `yesterday's screenshot is stale, not a stranger`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now - 86_400}|R4nd0m", now)
        assertEquals(Scanned.Stale("vgu-gate-01", 86_400L), read)
    }

    /**
     * A phone whose clock is a few seconds behind the cabinet's. Nobody did
     * anything wrong and nobody should be told they did.
     */
    @Test
    fun `a code from ten seconds in our future is ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now + 10}|R4nd0m", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now + 10), read)
    }

    /**
     * Far in the future is not a clock disagreeing. Refused as not ours, never
     * as stale: "scan again" would be a lie, because scanning it again gives
     * the same answer.
     */
    @Test
    fun `a code dated next week is not ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now + 604_800}|R4nd0m", now)
        assertEquals(Scanned.NotOurs, read)
    }

    /**
     * Everything a camera catches that we did not write. A person holding a
     * phone up in a lobby sweeps past posters, wifi codes and payment codes,
     * and the app must stay quiet through all of it.
     */
    @Test
    fun `other people's codes are not ours`() {
        val strangers = listOf(
            null,
            "",
            "   ",
            "https://vgu.edu.vn",
            "WIFI:S=VGU-Guest;T=WPA;P=letmein;;",
            "VGU2|vgu-gate-01|$now|R4nd0m",   // a format we have not written yet
            "vgu1|vgu-gate-01|$now|R4nd0m",   // marker is exact, not casual
            "VGU1|vgu-gate-01",               // two fields
            "VGU1|vgu-gate-01|$now",          // three - the shape before the server issued these
            "VGU1|vgu-gate-01|$now|R|extra",  // five
            "VGU1|vgu-gate-01|$now|",         // the random field, empty
            "VGU1||$now|R4nd0m",              // no cabinet
            "VGU1|vgu-gate-01|soon|R4nd0m",   // a word where the clock goes
            "VGU1|vgu-gate-01|0|R4nd0m",
            "VGU1|vgu-gate-01|-5|R4nd0m",
            "VGU1|../../etc/passwd|$now|R",   // an id that is trying something
            "VGU1|vgu gate 01|$now|R",        // spaces are not in an id
            "VGU1|${"a".repeat(65)}|$now|R",  // longer than any cabinet we name
        )
        strangers.forEach {
            assertEquals("read as something other than a stranger: $it", Scanned.NotOurs, readSessionCode(it, now))
        }
    }

    /** Hyphens are in every id we hand out, so they had better pass. */
    @Test
    fun `a plain id with digits and hyphens is fine`() {
        val read = readSessionCode("VGU1|vgu-library-2b|$now|R4nd0m", now)
        assertEquals(Scanned.Ours("vgu-library-2b", now), read)
    }

    /**
     * The cabinet screen must **carry** a code, never compose one.
     *
     * It composed its own until 2026-08-15. That cannot survive the server
     * checking codes: a code the server never issued is one the server must
     * refuse, so a screen that invents them turns every scan in the building
     * into a refusal, and nothing in either build says why.
     *
     * This does not check the *shape* any more - the server writes it now, and
     * `scripts/checkserver.py` compares a real issued code against this
     * reader. What is checked here is that the screen has not gone back to
     * making them up.
     */
    @Test
    fun `the cabinet screen asks the server for its code`() {
        val js = qrJs()
        assertTrue(
            "the cabinet's qr.js no longer asks the server for a session code",
            js.contains("/cabinet/session"),
        )
        assertTrue(
            "the cabinet's qr.js is composing a payload again instead of carrying one",
            !js.contains("FORMAT + \"|\""),
        )
    }

    // ---- what the app does about a code it has read ----

    @Test
    fun `a live code is worth asking the server about`() {
        val raw = "VGU1|vgu-back-gate|$now|R4nd0m"
        assertEquals(Pickup.Ask(raw), whatToDo(readSessionCode(raw, now), raw))
    }

    /**
     * The string goes on **untouched**.
     *
     * The server compares the whole code against what it issued, so anything
     * this app rebuilt from the parts it understood would differ by whatever
     * it did not - a longer random field, a fifth field added later - and
     * would be refused with nothing on either side saying why.
     */
    @Test
    fun `the code sent on is the code that was scanned`() {
        val raw = "VGU1|vgu-back-gate|$now|aVeryLongRandomFieldThatThisFileNeverInterprets"
        val next = whatToDo(readSessionCode(raw, now), raw)
        assertEquals(raw, (next as Pickup.Ask).code)
    }

    /**
     * **The app no longer answers this one.**
     *
     * Until P5-03 a live code at a cabinet with nothing of yours in it was
     * refused here, from a list of parcels the app was carrying. That check
     * protected nothing - it ran on the caller's phone, and anybody willing to
     * open a locker they do not own is willing to run a build without it - and
     * the list was stale from the moment it was fetched.
     *
     * So the code goes to the server, and `NO_PARCEL_HERE` comes back from the
     * one place that knows. Losing a check that never worked is the point of
     * the change, not a regression.
     */
    @Test
    fun `a cabinet with nothing of yours in it is still the server's answer`() {
        val raw = "VGU1|vgu-library|$now|R4nd0m"
        assertEquals(Pickup.Ask(raw), whatToDo(readSessionCode(raw, now), raw))
    }

    @Test
    fun `an expired code says scan again`() {
        val raw = "VGU1|vgu-back-gate|${now - 3_600}|R4nd0m"
        assertEquals(Pickup.ScanAgain, whatToDo(readSessionCode(raw, now), raw))
    }

    /**
     * An expired code is told to scan again whichever cabinet it is from, and
     * is never sent. Two reasons, and the second is the one that matters:
     * a round trip in a corridor is slow, and an answer that differed by
     * cabinet would let somebody with a dead code work out where your parcels
     * are without ever holding a live one.
     */
    @Test
    fun `an expired code never says whether a parcel is there`() {
        val mine = "VGU1|vgu-back-gate|${now - 3_600}|R4nd0m"
        val theirs = "VGU1|vgu-library|${now - 3_600}|R4nd0m"
        assertEquals(
            whatToDo(readSessionCode(theirs, now), theirs),
            whatToDo(readSessionCode(mine, now), mine),
        )
        assertEquals(Pickup.ScanAgain, whatToDo(readSessionCode(mine, now), mine))
    }

    @Test
    fun `a stranger's QR is not answered at all`() {
        val raw = "https://vgu.edu.vn"
        assertEquals(Pickup.KeepLooking, whatToDo(readSessionCode(raw, now), raw))
    }

    /**
     * Nothing the app decides is ever "a door opened".
     *
     * The one rule this file must never break, checked as a rule rather than
     * as a case: no input produces an answer that moves metal, because no such
     * answer exists any more.
     */
    @Test
    fun `no scanned string makes this app open anything`() {
        val everything = listOf(
            "VGU1|vgu-back-gate|$now|R4nd0m",
            "VGU1|vgu-library|${now - 3_600}|R4nd0m",
            "VGU1|somewhere-else|$now|R4nd0m",
            "https://vgu.edu.vn",
            "",
        )
        everything.forEach { raw ->
            val next = whatToDo(readSessionCode(raw, now), raw)
            assertTrue(
                "the app decided something other than asking, for $raw: $next",
                next is Pickup.Ask || next is Pickup.ScanAgain || next is Pickup.KeepLooking,
            )
        }
    }

    /** `src/cabinet/qr.js`, found by walking up from wherever tests run. */
    private fun qrJs(): String {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val hit = File(dir, "src/cabinet/qr.js")
            if (hit.isFile) return hit.readText()
            dir = dir.parentFile
        }
        throw AssertionError("src/cabinet/qr.js not found above ${File(".").absolutePath}")
    }
}
