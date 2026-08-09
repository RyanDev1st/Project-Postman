package otp

/**
 * Demo mode. Prints what would be sent and keeps the last message, so tests
 * (and a human watching the console) can read the code. Never used in
 * production: the route wiring only picks this when DEMO_MODE is on.
 */
class LogSms : SmsProvider {

    /** Last message that was "sent", or null before the first send. */
    var lastContent: String? = null

    override fun send(toE164: String, content: String): Boolean {
        lastContent = content
        println("DEMO OTP for $toE164: $content")
        return true
    }
}
