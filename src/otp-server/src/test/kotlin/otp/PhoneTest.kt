package otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneTest {

    @Test
    fun acceptsLocalFormat() {
        assertEquals("+84908619328", Phone.normalize("0908619328"))
    }

    @Test
    fun acceptsCountryCodeWithoutPlus() {
        assertEquals("+84908619328", Phone.normalize("84908619328"))
    }

    @Test
    fun acceptsE164WithPlus() {
        assertEquals("+84908619328", Phone.normalize("+84908619328"))
    }

    @Test
    fun ignoresSpacesDashesAndDots() {
        assertEquals("+84908619328", Phone.normalize(" 0908 619-328."))
    }

    @Test
    fun rejectsLandline() {
        assertNull(Phone.normalize("02866808866"))
    }

    @Test
    fun rejectsShortNumber() {
        assertNull(Phone.normalize("090861"))
    }

    @Test
    fun rejectsLetters() {
        assertNull(Phone.normalize("0908abc328"))
    }

    @Test
    fun rejectsEmpty() {
        assertNull(Phone.normalize(""))
    }
}
