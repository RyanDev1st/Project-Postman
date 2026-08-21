package vn.edu.vgu.smartlocker.server.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The two lines that decide whether a code ever reaches a phone.
 *
 * `asciiJson` builds the request; `speedSmsAccepted` reads the answer. Both
 * were wrong until a real token was pointed at the live API on 2026-08-21, and
 * both were wrong in a way no amount of reading would have shown. Every body
 * quoted below is one the API really returned that day.
 *
 * Free to run. Asking the provider the same questions needs an account, and
 * the ones that succeed cost money.
 */
class SpeedSmsAcceptedTest {

    @Test
    fun `the real success is a success`() {
        // The spelling is "success". This first accepted only "status":1, read
        // from a write-up - so every delivered message would have been called a
        // failure and nobody could have registered at all.
        val real =
            """{"status":"success","code":"00","data":{"email":"a@b.com","balance":2000,"currency":"VND"}}"""
        assertTrue(speedSmsAccepted(200, real))
    }

    @Test
    fun `the real refusal is not a success`() {
        val badToken =
            """{"name":"Unauthorized","message":"Your request was made with invalid credentials.","code":0,"status":401}"""
        assertFalse(speedSmsAccepted(401, badToken))

        // And it must still refuse if that body ever arrives under HTTP 200,
        // which is a thing gateways and captive portals do.
        assertFalse(speedSmsAccepted(200, badToken))
    }

    @Test
    fun `an error inside HTTP 200 is not a success`() {
        // Their API answers a body it cannot use with 200 and an error inside,
        // so the status line alone would have called these sent messages.
        assertFalse(
            speedSmsAccepted(200, """{"status":"error","code":"101","message":"Invalid or missing parameters"}"""),
        )
        assertFalse(speedSmsAccepted(200, """{"status":"error","message":"sender not found"}"""))
    }

    @Test
    fun `an unclear answer is a failure, not a maybe`() {
        // A code the user may never have received must not be usable.
        assertFalse(speedSmsAccepted(200, ""))
        assertFalse(speedSmsAccepted(200, "<html>Service unavailable</html>"))
        assertFalse(speedSmsAccepted(200, """{"status":0}"""))
        assertFalse(speedSmsAccepted(500, """{"status":"success"}"""))
    }

    @Test
    fun `the word success elsewhere in the body does not count`() {
        assertFalse(speedSmsAccepted(200, """{"status":"error","message":"no success"}"""))
        assertFalse(speedSmsAccepted(200, """{"result":"success"}"""))
    }

    @Test
    fun `a status of 1 is not read out of a longer number`() {
        // "status":100 and "status":12 were both read as 1 by the first
        // pattern, and 1xx is exactly what an error carries.
        assertFalse(speedSmsAccepted(200, """{"status":12}"""))
        assertFalse(speedSmsAccepted(200, """{"status":100}"""))
        assertTrue(speedSmsAccepted(200, """{"status":1,"code":"00"}"""))
        assertTrue(speedSmsAccepted(200, """{"status":"1"}"""))
    }

    @Test
    fun `Vietnamese text goes out as ASCII escapes`() {
        // Not cosmetic. Raw UTF-8 in the body makes their API answer
        // {"status":"error","code":"101"}, and every code this server sends is
        // Vietnamese - so raw UTF-8 meant no code could ever be delivered.
        val json = asciiJson("Mã xác thực của bạn là 123456. Có hiệu lực trong 5 phút.")

        assertTrue(json.all { it.code in 0x20..0x7E }, "left a byte above ASCII: $json")
        // Raw strings below, so these are literal backslash-u sequences - the
        // characters that go on the wire, not what Kotlin would decode.
        assertTrue(json.contains("""M\u00e3 x\u00e1c th\u1ef1c"""), json)
        assertTrue(json.contains("""ph\u00fat."""), json)
        // The digits a person types back must survive untouched.
        assertTrue(json.contains("123456"), json)
    }

    @Test
    fun `the quoting cannot be broken out of`() {
        // The message is ours, but the sender is configuration and a stray
        // quote in it would end the JSON string early and let the rest be read
        // as fields.
        assertEquals(""""a\"b"""", asciiJson("a\"b"))
        assertEquals(""""a\\b"""", asciiJson("a\\b"))
        assertEquals(""""a\nb"""", asciiJson("a\nb"))
        assertEquals("\"\"", asciiJson(""))
    }
}
