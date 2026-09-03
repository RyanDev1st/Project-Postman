package vn.edu.vgu.smartlocker.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Which language the app opens in when nobody has chosen one. Task P2-16.
 *
 * **Vietnamese, on every phone**, and that is a decision about the users
 * rather than about the code. This locker stands on a Vietnamese campus, most
 * of the people who will use it read Vietnamese more easily than English, and
 * a phone's language is often whatever it shipped with rather than a choice
 * anybody made. An English reader changes it in Settings once; a Vietnamese
 * reader handed an English app may never find the row that fixes it.
 *
 * Until 2026-09-03 this followed the phone, so an English phone opened an
 * English app - which is why every case below sets a different locale and
 * expects the same answer. `Locale.setDefault` is global to the JVM, so the
 * real default is put back after each case.
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

    /** The one that changed. An English phone gets a Vietnamese app too. */
    @Test
    fun `an English phone also opens the app in Vietnamese`() {
        Locale.setDefault(Locale.forLanguageTag("en-GB"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ofSystem())
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ofSystem())
    }

    /**
     * A language the app does not speak lands on Vietnamese as well, because
     * that is what `values/` holds now. There is no third answer to give.
     */
    @Test
    fun `a language the app does not speak lands on Vietnamese`() {
        Locale.setDefault(Locale.forLanguageTag("de-DE"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ofSystem())
        Locale.setDefault(Locale.forLanguageTag("ja-JP"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ofSystem())
    }

    /**
     * No locale at all - a JVM with a root default, which is what a stripped
     * container gives you. Still Vietnamese, and still not a crash.
     */
    @Test
    fun `no language at all is still Vietnamese`() {
        Locale.setDefault(Locale.ROOT)
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ofSystem())
    }

    /**
     * Both tags are BCP 47, which is what `values-en` resolves against.
     * Vietnamese needs no folder of its own any more: it is the default.
     */
    @Test
    fun `the tags are the ones Android resolves resources with`() {
        assertEquals("en", AppLanguage.ENGLISH.tag)
        assertEquals("vi", AppLanguage.VIETNAMESE.tag)
    }

    /** English is still offered, and still second. The picker draws both. */
    @Test
    fun `both languages are still on offer`() {
        assertEquals(
            listOf(AppLanguage.ENGLISH, AppLanguage.VIETNAMESE),
            AppLanguage.entries.toList(),
        )
    }
}
