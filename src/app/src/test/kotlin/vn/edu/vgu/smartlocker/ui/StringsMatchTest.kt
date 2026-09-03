package vn.edu.vgu.smartlocker.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Both languages hold the same names. Task P2-16.
 *
 * This mattered less when `values/` held English: a name missing from
 * `values-vi/` fell back to the English one, and a Vietnamese screen showed
 * one English line. It matters now. **`values/` holds Vietnamese and is the
 * fallback for everything**, so a name that exists only in `values-en/` has
 * nothing to fall back to - `stringResource` throws `Resource ID #0x0` and
 * the screen dies, on every phone that is not set to English. Which is all of
 * them, since the app opens in Vietnamese.
 *
 * Android's own `MissingTranslation` lint looks the other way round and would
 * not see it. This reads the two files and compares the names, which needs no
 * device and no lint run.
 */
class StringsMatchTest {

    private val res = File("src/main/res").takeIf { it.isDirectory }
        ?: File("src/app/src/main/res")

    private val vietnamese = File(res, "values/strings.xml")
    private val english = File(res, "values-en/strings.xml")

    private fun namesIn(file: File): Set<String> {
        assertTrue("no strings file at ${file.absolutePath}", file.isFile)
        return Regex("""<string name="([^"]+)"""")
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun `every name in English has a Vietnamese one`() {
        val missing = namesIn(english) - namesIn(vietnamese)
        assertEquals(
            "these would crash on a Vietnamese phone - values/ is the fallback: $missing",
            emptySet<String>(),
            missing,
        )
    }

    @Test
    fun `and every Vietnamese name has an English one`() {
        val missing = namesIn(vietnamese) - namesIn(english)
        assertEquals(
            "these would show in Vietnamese inside an English app: $missing",
            emptySet<String>(),
            missing,
        )
    }

    /**
     * The default set is the Vietnamese one, and this is what says so. If
     * somebody flips the folders back, the sentence below stops being true
     * and this test is the first thing to notice.
     */
    @Test
    fun `values holds Vietnamese and values-en holds English`() {
        assertTrue(
            "values/strings.xml is not the Vietnamese file",
            vietnamese.readText().contains("Tủ gửi hàng thông minh"),
        )
        assertTrue(
            "values-en/strings.xml is not the English file",
            english.readText().contains("Smart parcel locker"),
        )
        assertTrue(
            "values-vi/ is back - the default is meant to BE Vietnamese",
            !File(res, "values-vi").exists(),
        )
    }
}
