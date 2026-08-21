package vn.edu.vgu.smartlocker.server.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The line that decides whether a code is real.
 *
 * `speedSmsAccepted` answers one question - did the provider confirm it sent -
 * and the whole login rests on it. Say yes wrongly and a code nobody received
 * opens an account. Say no wrongly and nobody can register at all, while the
 * messages are still charged for.
 *
 * Every case here is free. Asking the provider the same questions costs 350
 * VND each and needs an account this team does not have yet.
 */
class SpeedSmsAcceptedTest {

    @Test
    fun `the real refusal is not a success`() {
        // Not invented. This is what api.speedsms.vn answered a bogus token on
        // 2026-08-21, byte for byte.
        val real = """{"name":"Unauthorized","message":"Your request was made with invalid credentials.","code":0,"status":401}"""
        assertFalse(speedSmsAccepted(401, real))

        // And it must still refuse if that body ever arrives under HTTP 200,
        // which is a thing gateways and captive portals do.
        assertFalse(speedSmsAccepted(200, real))
    }

    @Test
    fun `both documented spellings of success are accepted`() {
        assertTrue(speedSmsAccepted(200, """{"status":1,"code":"00"}"""))
        assertTrue(speedSmsAccepted(200, """{"status":"1"}"""))
        assertTrue(speedSmsAccepted(200, """{"status":"success","data":{"total_sms":1}}"""))
        assertTrue(speedSmsAccepted(200, """{ "status" : "success" }"""))
    }

    @Test
    fun `an unclear answer is a failure, not a maybe`() {
        // A code the user may never have received must not be usable. Each of
        // these is a real way an HTTP call comes back wrong.
        assertFalse(speedSmsAccepted(200, ""))
        assertFalse(speedSmsAccepted(200, "<html>Service unavailable</html>"))
        assertFalse(speedSmsAccepted(200, """{"status":"error","message":"out of balance"}"""))
        assertFalse(speedSmsAccepted(200, """{"status":0}"""))
        assertFalse(speedSmsAccepted(500, """{"status":"success"}"""))
    }

    @Test
    fun `the word success elsewhere in the body does not count`() {
        // The field is what matters, not the word. A message that happens to
        // contain "success" must not talk its way past this.
        assertFalse(speedSmsAccepted(200, """{"status":"error","message":"no success"}"""))
        assertFalse(speedSmsAccepted(200, """{"result":"success"}"""))
    }

    @Test
    fun `a status of 1 is not read out of a longer number`() {
        // `"status":401` starts with no 1, but `"status":100` and `"status":12`
        // would both be read as 1 by a looser pattern - and 1xx and 4xx are
        // exactly what an error carries.
        assertFalse(speedSmsAccepted(200, """{"status":12}"""))
        assertFalse(speedSmsAccepted(200, """{"status":100}"""))
    }
}
