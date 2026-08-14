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
 * The last test is the one that matters most. The other end of this contract
 * is `src/cabinet/qr.js`, in another language, in another folder, built by
 * another half of the team. Nothing in a compiler connects the two, so the
 * test reads that file and fails if its format marker ever stops matching
 * this one.
 */
class SessionCodeTest {

    /** A moment. Any moment; the code carries its own. */
    private val now = 1_786_686_322L

    @Test
    fun `a code the cabinet drew a second ago is ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now - 1}", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now - 1), read)
    }

    @Test
    fun `spaces around it are the camera's, not the cabinet's`() {
        val read = readSessionCode("  VGU1|vgu-gate-01|$now\n", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now), read)
    }

    /**
     * The cabinet redraws every 30 seconds and each code lives 60, so a code
     * being a little old is the normal case, not a fault.
     */
    @Test
    fun `a code from forty seconds ago is still ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now - 40}", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now - 40), read)
    }

    /**
     * 60 seconds of life plus 15 of slack for two clocks that disagree. At 75
     * it still passes; the slack is there to be used, not admired.
     */
    @Test
    fun `the last second of the window is still ours`() {
        val edge = CODE_GOOD_FOR_SECONDS + 15L
        val read = readSessionCode("VGU1|vgu-gate-01|${now - edge}", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now - edge), read)
    }

    @Test
    fun `a second past the window is stale`() {
        val past = CODE_GOOD_FOR_SECONDS + 16L
        val read = readSessionCode("VGU1|vgu-gate-01|${now - past}", now)
        assertEquals(Scanned.Stale("vgu-gate-01", past), read)
    }

    /**
     * A photograph of the screen, scanned later. Stale, and the screen says
     * "scan again" — which works, because the cabinet has drawn a new one.
     */
    @Test
    fun `yesterday's screenshot is stale, not a stranger`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now - 86_400}", now)
        assertEquals(Scanned.Stale("vgu-gate-01", 86_400L), read)
    }

    /**
     * A phone whose clock is a few seconds behind the cabinet's. Nobody did
     * anything wrong and nobody should be told they did.
     */
    @Test
    fun `a code from ten seconds in our future is ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now + 10}", now)
        assertEquals(Scanned.Ours("vgu-gate-01", now + 10), read)
    }

    /**
     * Far in the future is not a clock disagreeing. Refused as not ours, never
     * as stale: "scan again" would be a lie, because scanning it again gives
     * the same answer.
     */
    @Test
    fun `a code dated next week is not ours`() {
        val read = readSessionCode("VGU1|vgu-gate-01|${now + 604_800}", now)
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
            "VGU2|vgu-gate-01|$now",          // a format we have not written yet
            "vgu1|vgu-gate-01|$now",          // marker is exact, not casual
            "VGU1|vgu-gate-01",               // two fields
            "VGU1|vgu-gate-01|$now|extra",    // four
            "VGU1||$now",                     // no cabinet
            "VGU1|vgu-gate-01|soon",          // a word where the clock goes
            "VGU1|vgu-gate-01|0",
            "VGU1|vgu-gate-01|-5",
            "VGU1|../../etc/passwd|$now",     // an id that is trying something
            "VGU1|vgu gate 01|$now",          // spaces are not in an id
            "VGU1|${"a".repeat(65)}|$now",    // longer than any cabinet we name
        )
        strangers.forEach {
            assertEquals("read as something other than a stranger: $it", Scanned.NotOurs, readSessionCode(it, now))
        }
    }

    /** Hyphens are in every id we hand out, so they had better pass. */
    @Test
    fun `a plain id with digits and hyphens is fine`() {
        val read = readSessionCode("VGU1|vgu-library-2b|$now", now)
        assertEquals(Scanned.Ours("vgu-library-2b", now), read)
    }

    /**
     * The other end of the contract, in another language.
     *
     * `qr.js` writes `FORMAT + "|" + cabinetId + "|" + seconds`. If somebody
     * changes the marker or the separator there and not here, every scan in
     * the building becomes "not ours" and nothing in either build says why.
     * This is the only place the two halves are ever compared.
     */
    @Test
    fun `the cabinet screen writes the format this file reads`() {
        val js = qrJs()
        assertTrue(
            "the cabinet's qr.js no longer writes the VGU1 marker this reader expects",
            js.contains("\"VGU1\""),
        )
        assertTrue(
            "the cabinet's qr.js no longer joins its fields with a pipe",
            js.contains("FORMAT + \"|\" + cabinetId + \"|\" + seconds"),
        )
        // The 60 seconds this reader allows is the cabinet's own session
        // length. Both are read from settings; the default must agree.
        assertTrue(
            "the cabinet's session length no longer defaults to the $CODE_GOOD_FOR_SECONDS " +
                "seconds this reader allows",
            js.contains("|| $CODE_GOOD_FOR_SECONDS"),
        )
    }

    // ---- what the app does about a code it has read ----

    /** Both parcels are at the back gate, and nothing is at the library. */
    private val yours = mapOf("vgu-back-gate" to listOf("04", "07"))

    @Test
    fun `a live code at your cabinet opens your box`() {
        val read = readSessionCode("VGU1|vgu-back-gate|$now", now)
        assertEquals(Pickup.Open("04"), whatToDo(read, yours))
    }

    /** Two parcels there, and you already said which one you came for. */
    @Test
    fun `the box you tapped on Home is the one that opens`() {
        val read = readSessionCode("VGU1|vgu-back-gate|$now", now)
        assertEquals(Pickup.Open("07"), whatToDo(read, yours, want = "07"))
    }

    /** You tapped 07, then walked to a cabinet where only 04 is yours. */
    @Test
    fun `asking for a box that is not at this cabinet opens the one that is`() {
        val read = readSessionCode("VGU1|vgu-back-gate|$now", now)
        val onlyOne = mapOf("vgu-back-gate" to listOf("04"))
        assertEquals(Pickup.Open("04"), whatToDo(read, onlyOne, want = "07"))
    }

    /**
     * The fault this whole check exists for. Before the codes carried a real
     * cabinet id, any live code opened box 04 - so standing at the library and
     * scanning it announced a door at the back gate.
     */
    @Test
    fun `a cabinet with nothing of yours in it says so`() {
        val read = readSessionCode("VGU1|vgu-library|$now", now)
        assertEquals(Pickup.NoParcelHere, whatToDo(read, yours))
    }

    @Test
    fun `an expired code says scan again`() {
        val read = readSessionCode("VGU1|vgu-back-gate|${now - 3_600}", now)
        assertEquals(Pickup.ScanAgain, whatToDo(read, yours))
    }

    /**
     * An expired code is told to scan again whichever cabinet it is from.
     * Answering "no parcel for you here" would let somebody with a dead code
     * work out where your parcels are without ever holding a live one.
     */
    @Test
    fun `an expired code never says whether a parcel is there`() {
        val mine = readSessionCode("VGU1|vgu-back-gate|${now - 3_600}", now)
        val theirs = readSessionCode("VGU1|vgu-library|${now - 3_600}", now)
        assertEquals(whatToDo(theirs, yours), whatToDo(mine, yours))
        assertEquals(Pickup.ScanAgain, whatToDo(mine, yours))
    }

    @Test
    fun `a stranger's QR is not answered at all`() {
        val read = readSessionCode("https://vgu.edu.vn", now)
        assertEquals(Pickup.KeepLooking, whatToDo(read, yours))
    }

    /** Nothing waiting anywhere. Every cabinet is a cabinet with none of yours. */
    @Test
    fun `with no parcels at all, a live code still opens nothing`() {
        val read = readSessionCode("VGU1|vgu-back-gate|$now", now)
        assertEquals(Pickup.NoParcelHere, whatToDo(read, emptyMap()))
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
