package vn.edu.vgu.smartlocker.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Which language the app opens in when nobody has chosen one.
 *
 * This is the branch a Vietnamese student meets first and never sees a picker
 * for, so it is the one worth pinning. It went untested until the app was
 * checked on an emulator whose locale could not be changed without a reboot —
 * the check belongs here anyway, because it is a pure rule about a `Locale`
 * and needs no device to answer.
 *
 * `Locale.setDefault` is global to the JVM, so the real default is put back
 * after each case.
 */
class LanguageTest {

    private lateinit var was: Locale

    @Before
    fun remember() {
        was = Locale.getDefault()
    }

    @After
    fun restore() {
        Locale.setDefault(was)
    }

    @Test
    fun `a Vietnamese phone opens the app in Vietnamese`() {
        Locale.setDefault(Locale.forLanguageTag("vi-VN"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ofSystem())
    }

    /** The tag carries no region, so a Vietnamese speaker anywhere counts. */
    @Test
    fun `Vietnamese without a country still counts`() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ofSystem())
    }

    @Test
    fun `an English phone opens the app in English`() {
        Locale.setDefault(Locale.forLanguageTag("en-GB"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.ofSystem())
    }

    /**
     * A language the app does not speak falls back to English rather than to
     * nothing — English is what `values/` holds, so it is the only honest
     * answer for a third language.
     */
    @Test
    fun `a language the app does not speak falls back to English`() {
        Locale.setDefault(Locale.forLanguageTag("de-DE"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.ofSystem())
    }

    /** Both tags are BCP 47, which is what `values-vi` resolves against. */
    @Test
    fun `the tags are the ones Android resolves resources with`() {
        assertEquals("en", AppLanguage.ENGLISH.tag)
        assertEquals("vi", AppLanguage.VIETNAMESE.tag)
    }
}
