package otp

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProvidersTest {

    /** A tiny fake SpeedSMS endpoint, so no test touches the network. */
    private fun fakeSpeedSms(
        respondBody: String,
        respondStatus: Int = 200,
        record: (auth: String?, body: String?) -> Unit = { _, _ -> },
    ): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/index.php/sms/send") { ex ->
            val auth = ex.requestHeaders.getFirst("Authorization")
            val body = ex.requestBody.bufferedReader().readText()
            record(auth, body)
            val bytes = respondBody.toByteArray(Charsets.UTF_8)
            ex.sendResponseHeaders(respondStatus, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
        return server
    }

    @Test
    fun logSmsRemembersWhatWasSent() {
        val log = LogSms()
        assertNull(log.lastContent)
        assertTrue(log.send("+84908619328", "Mã xác thực của bạn là 123456."))
        assertEquals("Mã xác thực của bạn là 123456.", log.lastContent)
    }

    @Test
    fun speedSmsSendsBasicAuthAndE164WithoutPlus() {
        var seenAuth: String? = null
        var seenBody: String? = null
        val server = fakeSpeedSms("""{"status":1}""") { a, b -> seenAuth = a; seenBody = b }
        try {
            val ok = SpeedSms("tok123", "http://127.0.0.1:${server.address.port}")
                .send("+84908619328", "Mã xác thực của bạn là 123456.")
            assertTrue(ok)
            val expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString("tok123:x".toByteArray(Charsets.UTF_8))
            assertEquals(expectedAuth, seenAuth)
            assertTrue(seenBody!!.contains("\"to\":\"84908619328\""))
            assertTrue(seenBody!!.contains("\"type\":2"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun speedSmsFailsWhenStatusIsNotOne() {
        val server = fakeSpeedSms("""{"status":9,"message":"out of balance"}""")
        try {
            val ok = SpeedSms("tok123", "http://127.0.0.1:${server.address.port}")
                .send("+84908619328", "anything")
            assertFalse(ok)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun speedSmsFailsWhenServerErrors() {
        val server = fakeSpeedSms("""{"status":1}""", respondStatus = 500)
        try {
            val ok = SpeedSms("tok123", "http://127.0.0.1:${server.address.port}")
                .send("+84908619328", "anything")
            assertFalse(ok)
        } finally {
            server.stop(0)
        }
    }
}
