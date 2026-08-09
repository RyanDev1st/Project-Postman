package otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthStoreTest {

    /** A provider that records content and accepts or refuses on demand. */
    private class FakeSms(var accept: Boolean = true) : SmsProvider {
        var lastContent: String? = null
        override fun send(toE164: String, content: String): Boolean {
            lastContent = content
            return accept
        }
    }

    private fun store(accept: Boolean = true, now: () -> Long = { 1_000_000L }): Pair<AuthStore, FakeSms> {
        val sms = FakeSms(accept)
        return AuthStore(sms, now) to sms
    }

    @Test
    fun requestCodeSendsSixDigitsInVietnamese() {
        val (s, sms) = store()
        assertNull(s.requestCode("+84908619328"))
        val content = sms.lastContent!!
        assertTrue(content.matches(Regex("Mã xác thực của bạn là \\d{6}\\. Có hiệu lực trong 5 phút\\.")))
    }

    @Test
    fun requestCodeRefusesWithinCooldown() {
        var t = 1_000_000L
        val (s, _) = store(now = { t })
        assertNull(s.requestCode("+84908619328"))
        assertEquals("RATE_LIMITED", s.requestCode("+84908619328"))
        t += 61_000
        assertNull(s.requestCode("+84908619328"))
    }

    @Test
    fun requestCodeDoesNotStoreWhenSendFails() {
        val (s, _) = store(accept = false)
        assertEquals("SEND_FAILED", s.requestCode("+84908619328"))
        // Nothing stored: the code the user never received must not be usable.
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", "000000"))
    }

    @Test
    fun verifyCodeAcceptsTheSentCode() {
        val (s, sms) = store()
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        assertNull(s.verifyCode("+84908619328", code))
    }

    @Test
    fun wrongCodeConsumesTriesThenLocks() {
        val (s, sms) = store()
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        repeat(4) { assertEquals("WRONG_CODE", s.verifyCode("+84908619328", "000000")) }
        // Fifth wrong try uses the last try; the code is then gone.
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", "000000"))
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", code))
    }

    @Test
    fun codeIsSingleUse() {
        val (s, sms) = store()
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        assertNull(s.verifyCode("+84908619328", code))
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", code))
    }

    @Test
    fun expiredCodeIsRefused() {
        var t = 1_000_000L
        val (s, sms) = store(now = { t })
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        t += 301_000 // past the 5-minute TTL
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", code))
    }

    @Test
    fun tokenLifecycle() {
        val (s, _) = store()
        s.requestCode("+84908619328")
        val token = s.issueToken("+84908619328")

        assertEquals("+84908619328", s.phoneOf(token.value))
        assertEquals(1_000_000L + 30L * 24 * 3600 * 1000, token.expiresAtMs)

        val fresh = s.refresh(token.value)!!
        assertNotNull(fresh)
        assertNull(s.phoneOf(token.value)) // old token is dead
        assertEquals("+84908619328", s.phoneOf(fresh.value))

        s.revoke(fresh.value)
        assertNull(s.phoneOf(fresh.value))
    }

    @Test
    fun expiredTokenIsForgotten() {
        var t = 1_000_000L
        val (s, _) = store(now = { t })
        val token = s.issueToken("+84908619328")
        t += 31L * 24 * 3600 * 1000
        assertNull(s.phoneOf(token.value))
        assertNull(s.refresh(token.value))
    }

    @Test
    fun unknownTokenIsRefused() {
        val (s, _) = store()
        assertNull(s.phoneOf("nope"))
        assertNull(s.refresh("nope"))
    }
}
